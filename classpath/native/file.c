#include <jfvm.h>

#ifdef JFVM_WIN
  #include "file_win.c"
#endif

#ifdef JFVM_LNX
  #include "file_lnx.c"
#endif
