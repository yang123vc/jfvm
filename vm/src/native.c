#include <jfvm.h>

#ifdef JFVM_WIN
  #include "native_win.c"
#endif

#ifdef JFVM_LNX
  #include "native_lnx.c"
#endif
