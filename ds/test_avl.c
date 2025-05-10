/**
 * Randomly insert 1-100 with Fisher-Yates shuffle to test the AVL tree
*/
#include <stdlib.h>
#include <stdio.h>
#include "avl.h"

#define N 1000

inline void swap(int *x, int *y) {
    int temp = *x;
    *x = *y;
    *y = temp;
}

int main(void) {
    int arr[N];
    for (int i = 0; i < N; i++)
        arr[i] = i;

    avl_node_t *a = avl_node_new(N-1);
    avl_node_t *head = a;

    for (int i = N-2; i >= 1; i--) {
        /* Fisher-Yates shuffle */
        int j = rand() % i;

        /* Insert the shuffled value */
        avl_node_t *node = avl_node_new(arr[j]);
        avl_node_t* res = avl_insert(&head, node);
        if (res != 0) {
            printf("Duplicate key found %d\n", arr[j]);
            return EXIT_FAILURE;
        }

        swap(&arr[i], &arr[j]); /* Fisher-Yates shuffle */
    }

    avl_node_t *node = avl_node_new(arr[0]);
    avl_node_t* res = avl_insert(&head, node);
    if (res != 0) {
        printf("Duplicate key found %d\n", arr[0]);
        return EXIT_FAILURE;
    }

    printf("All keys inserted\n");

    /** Verify that the nodes are present */
    for (int i = 0; i < N; i++) {
        if (!avl_search(head, i)) {
            printf("Key not found %d\n", i);
        }
    }
    printf("All keys found\n");
    avl_destroy(head);

    return EXIT_SUCCESS;
}
