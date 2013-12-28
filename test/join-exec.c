#include "syscall.h"

char* file[10];
int argc;
char* argv[1];
int* save;

int main()
{
	file[0] = 'e';
	file[1] = 'x';
	file[2] = 'i';
	file[3] = 't';
	file[4] = '.';
	file[5] = 'c';
	file[6] = 'o';
	file[7] = 'f';
	file[8] = 'f';
	argc = 0;
	return join(exec(file, argc, argv), save);
}
