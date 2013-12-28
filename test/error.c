#include "syscall.h"

int error;

int main()
{
	error = 1/0;
	return 0;
}
