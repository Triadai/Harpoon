#ifndef TEST_H
#define TEST_H
void initializeanalysis();
void doanalysis();
void resetanalysis();
void addmapping(char *, void *,char *);
void addintmapping(char *key, int value);
void alloc(void *ptr,int size);
void dealloc(void *ptr);
void *ourcalloc(size_t nmemb, size_t size);
void *ourmalloc(size_t size);
void ourfree(void *ptr);
void *ourrealloc(void *ptr, size_t size);
#endif