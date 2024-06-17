#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>
#include <termios.h>
#include <unistd.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <stdbool.h>
#include <pthread.h>
#include <stdarg.h>

// Settings - BEGIN

#define SOCKET_SERVER_PORT 8080
#define HEARTBEAT_TIMEOUT_MS 5000
#define HEARTBEAT_SENTINEL "<heartbeat>"
#define HEARTBEAT_SENTINEL_LENGTH strlen(HEARTBEAT_SENTINEL)

#define SERIAL_BAUD_RATE B115200
#define SERIAL_DEVICE_PATH "/dev/ttyUSB2"
#define SOCKET_AND_SERIAL_BUFFER_SIZE 1024

#define PRINT_SOCKET_IO_HEX_AND_BINARY

const char *occupied_message = "There's already a client in control";
const char *heartbeat_timeout_message = "Your heartbeat timeout elapsed";
const char *serial_down_message = "Serial port is or became unavailable, shutting down";

// Settings - END

#define BYTE_TO_BINARY_PATTERN "%c%c%c%c%c%c%c%c"
#define BYTE_TO_BINARY(byte)  \
  ((byte) & 0x80 ? '1' : '0'), \
  ((byte) & 0x40 ? '1' : '0'), \
  ((byte) & 0x20 ? '1' : '0'), \
  ((byte) & 0x10 ? '1' : '0'), \
  ((byte) & 0x08 ? '1' : '0'), \
  ((byte) & 0x04 ? '1' : '0'), \
  ((byte) & 0x02 ? '1' : '0'), \
  ((byte) & 0x01 ? '1' : '0')

int controlling_client_fd = -1;
char socket_read_buffer[SOCKET_AND_SERIAL_BUFFER_SIZE];
struct timeval controlling_client_last_heartbeat;

int serial_port_fd = -1;
char serial_port_read_buffer[SOCKET_AND_SERIAL_BUFFER_SIZE];

// ====================================================================================================
// Utilities
// ====================================================================================================

void start_thread(void *(*function)(void *), void *parameter)
{
  pthread_t thread;
  pthread_create(&thread, NULL, function, parameter);
}

void log_prefixed_printf(const char *format, ...)
{
  char output_buffer[256];

  va_list args;
  va_start(args, format);
  vsnprintf(output_buffer, sizeof(output_buffer), format, args);
  va_end(args);

  time_t current_time = time(NULL);
  struct tm current_local_time = *localtime(&current_time);

  printf(
    "[%02d.%02d.%d %02d:%02d:%02d] %s",
    current_local_time.tm_mday,
    current_local_time.tm_mon + 1,
    current_local_time.tm_year + 1900,
    current_local_time.tm_hour,
    current_local_time.tm_min,
    current_local_time.tm_sec,
    output_buffer
  );
}

void print_characters_hex_and_binary(const char *text, int length)
{
  for (int i = 0; i < length; i++)
  {
    printf(" %02X " BYTE_TO_BINARY_PATTERN, (int) *text, BYTE_TO_BINARY(*text));
    text++;
  }
}

void print_characters_readable(const char *text, int length)
{
  for (int i = 0; i < length; i++)
  {
    char c = *text;

    if (c < 32)
    {
      if (c == '\n')
        fputs("\\n", stdout);
      else if (c == '\r')
        fputs("\\r", stdout);
      else if (c == '\t')
        fputs("\\t", stdout);
      else
        printf("\\x%02X", (int) c);
    }

    else
      putchar(c);

    text++;
  }
}

// ====================================================================================================
// Socket Server
// ====================================================================================================

void disconnect_client(const char *message)
{
  if (controlling_client_fd < 0)
    return;

  if (message)
  {
    send(controlling_client_fd, message, strlen(message), 0);
  }

  close(controlling_client_fd);
  controlling_client_fd = -1;

  log_prefixed_printf("Disconnected client: %s\n", message ? message : "No reason given");
}

void *start_heartbeat_watcher(void *parameter)
{
  log_prefixed_printf("Starting heartbeat watcher\n");

  while (1)
  {
    if (controlling_client_fd < 0)
    {
      log_prefixed_printf("Stopping heartbeat watcher\n");
      break;
    }

    struct timeval now;
    gettimeofday(&now, NULL);

    long heartbeat_ellapsed_us = (
      (now.tv_sec - controlling_client_last_heartbeat.tv_sec) * 1000 * 1000 +
      now.tv_usec - controlling_client_last_heartbeat.tv_usec
    );

    if (heartbeat_ellapsed_us >= HEARTBEAT_TIMEOUT_MS * 1000)
    {
      disconnect_client(heartbeat_timeout_message);
      break;
    }

    usleep(HEARTBEAT_TIMEOUT_MS * 1000 / 2);
  }

  return NULL;
}

void *start_client_receive_loop(void *parameter)
{
  log_prefixed_printf("Starting client receive loop\n");

  while (1)
  {
    if (controlling_client_fd < 0)
    {
      log_prefixed_printf("Stopping client receive loop\n");
      break;
    }

    int read_bytes = read(controlling_client_fd, socket_read_buffer, sizeof(socket_read_buffer));

    if (read_bytes <= 0)
    {
      if (controlling_client_fd >= 0)
      {
        log_prefixed_printf("Client read failed, disconnecting client\n");
        disconnect_client(NULL);
      }
      break;
    }

    socket_read_buffer[read_bytes] = 0;

    // TODO: The heartbeat consumer section really needs to be tested thoroughly

    char *message_head = socket_read_buffer;
    int message_length = read_bytes;
    bool contained_heartbeat = false;

    // Consume leading heartbeats
    while (strncmp(message_head, HEARTBEAT_SENTINEL, HEARTBEAT_SENTINEL_LENGTH) == 0)
    {
      message_head += HEARTBEAT_SENTINEL_LENGTH;
      message_length -= HEARTBEAT_SENTINEL_LENGTH;
      contained_heartbeat = true;
    }

    // Consume trailing heartbeats
    while (
      message_length >= HEARTBEAT_SENTINEL_LENGTH &&
      strncmp(message_head + message_length - HEARTBEAT_SENTINEL_LENGTH, HEARTBEAT_SENTINEL, HEARTBEAT_SENTINEL_LENGTH) == 0
    )
    {
      message_length -= HEARTBEAT_SENTINEL_LENGTH;
      message_head[message_length] = 0;
      contained_heartbeat = true;
    }

    if (contained_heartbeat)
      gettimeofday(&controlling_client_last_heartbeat, NULL);

    // Was just made up of heartbeat(s)
    if (message_length == 0)
      continue;

    if (serial_port_fd < 0)
    {
      disconnect_client(serial_down_message);
      exit(1);
    }

    write(serial_port_fd, message_head, message_length);

    log_prefixed_printf("SER_W(%d) ->", message_length);
    print_characters_readable(message_head, message_length);
    puts("<-");

    #ifdef PRINT_SOCKET_IO_HEX_AND_BINARY
    log_prefixed_printf("SER_WB(%d)", message_length);
    print_characters_hex_and_binary(message_head, message_length);
    puts("");
    #endif
  }

  return NULL;
}

void *handle_new_client(void *parameter)
{
  int client_fd = *((int *) parameter);

  if (controlling_client_fd >= 0)
  {
    send(client_fd, occupied_message, strlen(occupied_message), 0);
    close(client_fd);
    log_prefixed_printf("Successfully closed this client socket due to control being actively occupied already\n");
    return NULL;
  }

  controlling_client_fd = client_fd;
  gettimeofday(&controlling_client_last_heartbeat, NULL);

  start_thread(start_client_receive_loop, NULL);
  start_thread(start_heartbeat_watcher, NULL);

  log_prefixed_printf("Successfully gave control to this client socket\n");
  return NULL;
}

void *start_socket_server(void *parameter)
{
  int server_fd;
  if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0)
  {
    perror("socket failed");
    exit(EXIT_FAILURE);
  }

  int opt = 1;
  if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &opt, sizeof(opt)))
  {
    perror("setsockopt failed");
    exit(EXIT_FAILURE);
  }

  struct sockaddr_in address;
  int address_length = sizeof(address);

  address.sin_family = AF_INET;
  address.sin_addr.s_addr = INADDR_ANY;
  address.sin_port = htons(SOCKET_SERVER_PORT);

  if (bind(server_fd, (struct sockaddr*)&address, sizeof(address)) < 0)
  {
    perror("bind failed");
    exit(EXIT_FAILURE);
  }

  if (listen(server_fd, 3) < 0)
  {
    perror("listen failed");
    exit(EXIT_FAILURE);
  }

  log_prefixed_printf("Socket server is bound and listening\n");

  int new_socket;
  while (1)
  {
    if ((new_socket = accept(server_fd, (struct sockaddr*) &address, (socklen_t*) &address_length)) < 0)
    {
      log_prefixed_printf("Failed to accept a client socket, skipping\n");
      continue;
    }

    log_prefixed_printf("Successfully accepted a client socket\n");
    start_thread(handle_new_client, (void *) &new_socket);
  }
}

// ====================================================================================================
// Serial Device
// ====================================================================================================

void start_serial_receive_loop()
{
  log_prefixed_printf("Starting serial receive loop\n");

  while (1)
  {
    if (serial_port_fd < 0)
    {
      log_prefixed_printf("Stopping serial receive loop\n");
      break;
    }

    int read_bytes = read(serial_port_fd, serial_port_read_buffer, sizeof(serial_port_read_buffer));

    if (read_bytes < 0)
    {
      log_prefixed_printf("Serial read failed, closing connection\n");
      close(serial_port_fd);
      serial_port_fd = -1;
      break;
    }

    serial_port_read_buffer[read_bytes] = 0;

    log_prefixed_printf("SER_R(%d) ->", read_bytes);
    print_characters_readable(serial_port_read_buffer, read_bytes);
    puts("<-");

    #ifdef PRINT_SOCKET_IO_HEX_AND_BINARY
    log_prefixed_printf("SER_RB(%d)", read_bytes);
    print_characters_hex_and_binary(serial_port_read_buffer, read_bytes);
    puts("");
    #endif

    if (controlling_client_fd < 0)
      continue;

    write(controlling_client_fd, serial_port_read_buffer, read_bytes);
  }
}

void open_serial_device()
{
  serial_port_fd = open(SERIAL_DEVICE_PATH, O_RDWR);

  if (serial_port_fd < 0) {
    log_prefixed_printf("Error %i from open: %s\n", errno, strerror(errno));
    exit(EXIT_FAILURE);
  }

  struct termios tty;

  if(tcgetattr(serial_port_fd, &tty) != 0) {
    log_prefixed_printf("Error %i from tcgetattr: %s\n", errno, strerror(errno));
    exit(EXIT_FAILURE);
  }

  tty.c_cflag &= ~PARENB; // Clear parity bit, disabling parity (most common)
  tty.c_cflag &= ~CSTOPB; // Clear stop field, only one stop bit used in communication (most common)
  tty.c_cflag &= ~CSIZE; // Clear all bits that set the data size 
  tty.c_cflag |= CS8; // 8 bits per byte (most common)
  tty.c_cflag &= ~CRTSCTS; // Disable RTS/CTS hardware flow control (most common)
  tty.c_cflag |= CREAD | CLOCAL; // Turn on READ & ignore ctrl lines (CLOCAL = 1)

  tty.c_lflag &= ~ICANON;
  tty.c_lflag &= ~ECHO; // Disable echo
  tty.c_lflag &= ~ECHOE; // Disable erasure
  tty.c_lflag &= ~ECHONL; // Disable new-line echo
  tty.c_lflag &= ~ISIG; // Disable interpretation of INTR, QUIT and SUSP
  tty.c_iflag &= ~(IXON | IXOFF | IXANY); // Turn off s/w flow ctrl
  tty.c_iflag &= ~(IGNBRK|BRKINT|PARMRK|ISTRIP|INLCR|IGNCR|ICRNL); // Disable any special handling of received bytes

  tty.c_oflag &= ~OPOST; // Prevent special interpretation of output bytes (e.g. newline chars)
  tty.c_oflag &= ~ONLCR; // Prevent conversion of newline to carriage return/line feed
  // tty.c_oflag &= ~OXTABS; // Prevent conversion of tabs to spaces (NOT PRESENT ON LINUX)
  // tty.c_oflag &= ~ONOEOT; // Prevent removal of C-d chars (0x004) in output (NOT PRESENT ON LINUX)

  // http://unixwiz.net/techtips/termios-vmin-vtime.html
  tty.c_cc[VTIME] = 5; // How long to wait on data bursts to decide if more is to come, in deciseconds
  tty.c_cc[VMIN] = 1; // Reading should block until data is available (at least one byte)

  cfsetispeed(&tty, SERIAL_BAUD_RATE);
  cfsetospeed(&tty, SERIAL_BAUD_RATE);

  if (tcsetattr(serial_port_fd, TCSANOW, &tty) != 0) {
    log_prefixed_printf("Error %i from tcsetattr: %s\n", errno, strerror(errno));
    exit(EXIT_FAILURE);
  }

  log_prefixed_printf("Serial device opened successfully\n");
}

// ====================================================================================================
// Program Entry Point
// ====================================================================================================

int main()
{
  open_serial_device();
  start_thread(start_socket_server, NULL);
  start_serial_receive_loop();
  return 0;
}
