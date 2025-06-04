#include <mpi.h>
#include <omp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define KEY_LENGTH 16
#define HEADER_SIZE 54

void xor_encrypt(unsigned char* data, int len, const unsigned char* key) {
    #pragma omp parallel for
    for (int i = 0; i < len; i++) {
        data[i] ^= key[i % KEY_LENGTH];
    }
}

int main(int argc, char** argv) {
    MPI_Init(&argc, &argv);

    int rank, size;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    if (argc != 4) {
        if (rank == 0)
            fprintf(stderr, "Usage: %s <encryption|decryption> <ECB|CBC> <key> <bmp_file>\n", argv[0]);
        MPI_Finalize();
        return 1;
    }

    const char* operation = argv[1];
    const char* key_str = argv[2];
    const char* filename = argv[3];
    int is_decryption = (strcmp(operation, "decryption") == 0);

    if (strlen(key_str) != KEY_LENGTH) {
        if (rank == 0)
            fprintf(stderr, "Key must be 16 characters.\n");
        MPI_Finalize();
        return 1;
    }

    unsigned char key[KEY_LENGTH];
    memcpy(key, key_str, KEY_LENGTH);

    unsigned char* buffer = NULL;
    int file_size = 0;

    if (rank == 0) {
        FILE* f = fopen(filename, "rb");
        if (!f) { perror("Cannot open BMP"); MPI_Abort(MPI_COMM_WORLD, 1); }

        fseek(f, 0, SEEK_END); file_size = ftell(f); rewind(f);
        buffer = malloc(file_size);
        fread(buffer, 1, file_size, f);
        fclose(f);
    }

    MPI_Bcast(&file_size, 1, MPI_INT, 0, MPI_COMM_WORLD);
    if (rank != 0) buffer = malloc(file_size);
    MPI_Bcast(buffer, file_size, MPI_UNSIGNED_CHAR, 0, MPI_COMM_WORLD);

    int chunk = (file_size - HEADER_SIZE) / size;
    int start = HEADER_SIZE + rank * chunk;
    int end = (rank == size - 1) ? file_size : start + chunk;

    // Apply XOR (same for encrypt/decrypt)
    xor_encrypt(buffer + start, end - start, key);

    MPI_Gather(rank == 0 ? MPI_IN_PLACE : buffer + start,
               end - start, MPI_UNSIGNED_CHAR,
               buffer + HEADER_SIZE, end - start, MPI_UNSIGNED_CHAR,
               0, MPI_COMM_WORLD);

    if (rank == 0) {
        FILE* out = fopen("output.bmp", "wb");
        fwrite(buffer, 1, file_size, out);
        fclose(out);
        printf("Saved result to output.bmp\n");
    }

    free(buffer);
    MPI_Finalize();
    return 0;
}
