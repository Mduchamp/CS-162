#include "syscall.h"

int main()
{
	char* file[10]
	file[0] = 'e';
	file[1] = 'r';
	file[2] = 'r';
	file[3] = 'o';
	file[4] = 'r';
	file[5] = '.';
	file[6] = 'c';
	file[7] = '0';
	file[8] = 'f';
	file[9] = 'f';
	int argc = 0;
	char* argv[0];
	int* save;
	return join(exec(file, argc, argv), save);
}