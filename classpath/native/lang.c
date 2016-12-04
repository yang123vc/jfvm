#include <jfvm.h>

//misc java.lang functions

void java_java_lang_Float_intBitsToFloat(JVM *jvm, Slot *args) {
  args[0].type = 'F';
}

void java_java_lang_Float_floatToIntBits(JVM *jvm, Slot *args) {
  args[0].type = 'I';
}

void java_java_lang_Double_longBitsToDouble(JVM *jvm, Slot *args) {
  args[0].type = 'D';
}

void java_java_lang_Double_doubleToLongBits(JVM *jvm, Slot *args) {
  args[0].type = 'J';
}

void java_java_util_Date_now(JVM *jvm, Slot *args) {
  //TODO
}
