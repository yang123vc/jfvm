/**
 * Java ByteCode to C compiler.
 *
 * Requires gcc 4.7.0+
 *
 * @author pquiring
 *
 */

import java.io.*;
import java.util.*;

public class Compiler {
  private ByteArrayInputStream in;
  private DataInputStream dis;
  private StringBuffer cls_pre;
  private StringBuffer mths;

  private StringBuffer mth_pre;
  private StringBuffer mth;

  private int pc;
  private int localCount;
  private int stackCount;
  private int argsCount;
  private boolean isVoid;
  private ArrayList<String> classes;
  private int jsridx;
  private int syncidx;
  private VMClass cls;
  private ClassPool clspool;

  public void startClass(ClassPool clspool) throws Exception {
    this.clspool = clspool;
    classes = new ArrayList<String>();
    cls_pre = new StringBuffer();
    mths = new StringBuffer();
    syncidx = 0;
  }

  public String finishClass() throws Exception {
    addClasses();
//    if (!cls.isInterface) {
      object_init_vtable();
//    }
    cls_pre.append(mths.toString());
    return cls_pre.toString();
  }

  /** Generates C code for class (reflection data)
   */
  public void compileClass(VMClass cls) throws Exception {
    this.cls = cls;
    addClass(cls.name);  //ensure this class is first classref_0
    try {
      //generate includes
      cls_pre.append("#include <jfvm.h>\n");
      cls_pre.append("#include <math.h>\n");
      cls_pre.append("static void object_clsinit(JVM *jvm, Slot *stack);\n");
      cls_pre.append("static void object_init_vtable(JVM *jvm, Slot *stack);\n");
      //forward decl class itself
      cls_pre.append("extern Class " + cls.cname + ";\n");

      //generate interface list
      cls_pre.append("static const char *class_interfaces[] = {\n");
      int icnt = cls.InterfaceList.length;
      for(int a=0;a<icnt;a++) {
        Interface iface = cls.InterfaceList[a];
        if (a > 0) cls_pre.append(",");
        cls_pre.append(quoteString(iface.name));
      }
      if (icnt > 0) {
        cls_pre.append(",");
      }
      cls_pre.append("NULL};\n");  //null terminate list

      //generate interface_class list
      cls_pre.append("static Class *class_interfaces_class[] = {\n");
      for(int a=0;a<icnt;a++) {
        if (a > 0) cls_pre.append(",");
        cls_pre.append("NULL");
      }
      if (icnt > 0) {
        cls_pre.append(",");
      }
      cls_pre.append("NULL};\n");  //null terminate list

      int smcnt = cls.MethodList.length;
      //output forward decl for methods
      for(int a=0;a<smcnt;a++) {
        Method m = cls.MethodList[a];
        cls_pre.append("void " + m.cname + "(JVM *jvm, Slot *args); //" + m.name_desc + "\n");
      }

      cls_pre.append("static Method class_methods[] = {\n");
      int mcnt = cls.MethodList.length;
      boolean first = true;
      for(int a=0;a<mcnt;a++) {
        Method m = cls.MethodList[a];
        if (m.isStatic) continue;
//        if (m.isOverrides) continue;
        if (first) {first = false;} else {cls_pre.append(",");}
        cls_pre.append("{\n");
        cls_pre.append("  .name = " + quoteString(m.name) + "\n");
        cls_pre.append(", .desc = " + quoteString(m.desc) + "\n");
        cls_pre.append(", .name_desc = " + quoteString(m.name_desc) + "\n");
        cls_pre.append(", .flgs = " + m.flgs + "\n");
//        cls_pre.append(", .local = " + m.local + "\n");
        if (m.isAbstract) {
          cls_pre.append(", .method = NULL\n");
        } else {
          cls_pre.append(", .method = &" + m.cname + "\n");
        }
        cls_pre.append(", .offset = " + m.objidx + "\n");
        cls_pre.append("}\n");
      }
      if (!first) {
        cls_pre.append(",");
      }
      cls_pre.append("{.name = NULL}};\n");  //null terminate list

      cls_pre.append("static Method class_static_methods[] = {\n");
      first = true;
      for(int a=0;a<smcnt;a++) {
        Method m = cls.MethodList[a];
        if (!m.isStatic) continue;
//        if (m.isLambda) continue;
        if (first) {first = false;} else {cls_pre.append(",");}
        cls_pre.append("{\n");
        cls_pre.append("  .name = " + quoteString(m.name) + "\n");
        cls_pre.append(", .desc = " + quoteString(m.desc) + "\n");
        cls_pre.append(", .name_desc = " + quoteString(m.name_desc) + "\n");
        cls_pre.append(", .flgs = " + m.flgs + "\n");
//        cls_pre.append(", .local = " + m.local + "\n");
        cls_pre.append(", .method = &" + m.cname + "\n");
        cls_pre.append(", .offset = " + m.objidx + "\n");
        cls_pre.append("}\n");
      }
      if (!first) {
        cls_pre.append(",");
      }
      cls_pre.append("{.name = NULL}};\n");  //null terminate list

      cls_pre.append("static Field class_fields[] = {\n");
      int fcnt = cls.FieldList.length;
      first = true;
      for(int a=0;a<fcnt;a++) {
        Field f = cls.FieldList[a];
        if (f.isStatic) continue;
        if (first) {first = false;} else {cls_pre.append(",");}
        cls_pre.append("{\n");
        cls_pre.append("  .name = " + quoteString(f.name) + "\n");
        cls_pre.append(", .desc = " + quoteString(f.desc) + "\n");
        cls_pre.append(", .name_desc = " + quoteString(f.name + "$" + f.desc) + "\n");
        cls_pre.append(", .flgs = " + f.flgs + "\n");
        cls_pre.append(", .offset = " + f.objidx + "\n");
        cls_pre.append(", .i64 = 0\n");  //init value (not used here)
        cls_pre.append("}\n");
      }
      if (!first) {
        cls_pre.append(",");
      }
      cls_pre.append("{.name = NULL}};\n");  //null terminate list

      cls_pre.append("static Field class_static_fields[] = {\n");
      int sfcnt = cls.FieldList.length;
      first = true;
      for(int a=0;a<sfcnt;a++) {
        Field f = cls.FieldList[a];
        if (!f.isStatic) continue;
        if (first) {first = false;} else {cls_pre.append(",");}
        cls_pre.append("{\n");
        cls_pre.append("  .name = " + quoteString(f.name) + "\n");
        cls_pre.append(", .desc = " + quoteString(f.desc) + "\n");
        cls_pre.append(", .name_desc = " + quoteString(f.name + "$" + f.desc) + "\n");
        cls_pre.append(", .flgs = " + f.flgs + "\n");
        cls_pre.append(", .offset = " + f.objidx + "\n");
        cls_pre.append(", .i64 = 0\n");  //init value
        cls_pre.append("}\n");
      }
      if (!first) {
        cls_pre.append(",");
      }
      cls_pre.append("{.name = NULL}};\n");  //null terminate list

      //generate runtime class definition
      cls_pre.append("Class " + cls.cname + " = {\n");
      cls_pre.append(" .name=" + quoteString(cls.name) + "\n");
      if (cls.name.equals("java/lang/Object")) {
        cls_pre.append(", .super = NULL\n");
      } else {
        cls_pre.append(", .super = " + quoteString(cls.super_name) + "\n");
      }
      cls_pre.append(", .super_class = NULL\n");  //patched at load
      if (!cls.isInterface)
        cls_pre.append(", .object_clinit = &" + cls.getMethod("<clinit>()V").cname + "\n");
      else
        cls_pre.append(", .object_clinit = NULL\n");
      cls_pre.append(", .interfaces = &class_interfaces\n");
      cls_pre.append(", .interfaces_class = &class_interfaces_class\n");
      cls_pre.append(", .methods = &class_methods\n");
      cls_pre.append(", .static_methods = &class_static_methods\n");
      cls_pre.append(", .fields = &class_fields\n");
      cls_pre.append(", .static_fields = &class_static_fields\n");
      cls_pre.append(", .init_vtable = &object_init_vtable\n");
      cls_pre.append(", .flgs = " + cls.access_flags + "\n");
      cls_pre.append(", .size = " + cls.getFullSize() + " * sizeof(void*)\n");
      cls_pre.append(", .object_clinit_reflck = 0\n");
      cls_pre.append("};\n");
    } catch (Exception e) {
      e.printStackTrace(System.out);
    }
  }

  /** Generates C code for method.
   */
  public void compileMethod(VMClass cls, Method method) {
    boolean wide;
    int idx, val, pad, opc, defaultOffset, cnt, len;
    VMClass xcls;
    Method xmethod;
    Field xfield;
    Const constRef;
    ConstFieldRef fieldRef;
    ConstMethodRef methodRef;
    ConstClass classRef;
    ConstInterfaceRef ifaceRef;
    String scls;
    String sfield;
    String smethod;
    String type;
    String slot;
    jsridx = 0;
    mth_pre = new StringBuffer();
    mth = new StringBuffer();
    try {
      AttrCode code = method.getCode();
      in = new ByteArrayInputStream(code.code);
      dis = new DataInputStream(in);
      pc = 0;
      wide = false;
      mth_pre.append("void " + method.cname);
      mth_pre.append("(JVM *jvm, Slot *args) {\n");
      isVoid = method.isVoid();
      localCount = method.xlocal;
      stackCount = code.max_stack;
      argsCount = method.getArgsCount();  //includes 'this'
      if (argsCount == 0 && !isVoid) { argsCount++; } //need a place to store return value
      mth_pre.append("  //" + method.name + method.desc + ",args=" + argsCount + ",locals=" + localCount + ",stack=" + stackCount + "\n");
      mth.append("#ifdef JFVM_DEBUG_METHOD\n");
      if (!method.isStatic)
        mth.append("  printf(\"m  start:%p:%p:%s\\n\", jvm->thread, args[0].obj, " + quoteString(method.cname) + ");\n");
      else
        mth.append("  printf(\"m  start:%p:%s\\n\", jvm->thread, " + quoteString(method.cname) + ");\n");
      mth.append("#endif\n");
      preMethod(method);
      if (method.isSync) {
        mth.append("  jfvm_mutex_lock(method_mutex_" + syncidx + ");\n");
        mth.append("  jfvm_usync_add(jvm, &method_usync_" + syncidx + ",NULL);\n");
      }
      addExceptions(code);
      if (method.name.equals("<clinit>")) {
        mth.append("  object_clsinit(jvm, args);\n");
      }
      if (method.isLambda) {
        //lambda functions are static but code generated calls them like they are virtual ???
        //there must be some bridge function somewhere???
        remove(method.getArgsCount());
      }
      while (in.available() > 0) {
        mth.append("pc_" + pc + ":\n");
        checkException(code);
        opc = pc;
        //read java byte code
        int bytecode = readByte() & 0xff;
        if (bytecode >= mnemonics.length) {
          mth.append("// 0x" + Integer.toString(bytecode, 16) + ": Unknown\n");
          System.out.println("Error:Unknown bytecode:ox" + Integer.toString(bytecode, 16));
          continue;
        }
//        if (jfvmc.debug) mth.append("  printf(\"pc=%d %s %d %s\\n\", " + opc + "," + quoteString(mnemonics[bytecode]) + ",stackpos, " + quoteString(method.cname) + ");\n");
        mth.append("// 0x" + Integer.toString(bytecode, 16) + ":" + mnemonics[bytecode] + "\n");
        mth.append("  umethod->line=" + opc + ";\n");  //TODO : this is pc, should be line #
        if (bytecode == 0xc4) {
          wide = true;
          bytecode = readByte();
        } else {
          wide = false;
        }
        switch (bytecode) {
          case 0x32:  //aaload - load object from object array
            // arrayref, index -> object
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  jfvm_arc_get(jvm, &temp[2].obj, &temp[1].obj->array->objs[temp[0].i32]);\n");
            mth.append("  temp[2].type = 'A';\n");
            pushObject(2);
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x53:  //aastore
            // arrayref, index, object ->
            popObject(2);  //object
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  jfvm_arc_put(jvm, &temp[2].obj->array->objs[temp[0].i32], &temp[1].obj);\n");
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x01:  //aconst_null
            pushNULL();
            break;
          case 0x19:  //aload
            if (wide) {
              idx = readShort() & 0xff;
            } else {
              idx = readByte() & 0xffff;
            }
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].obj = " + slot + "[" + idx + "].obj;\n");
            mth.append("  stack[stackpos].type = " + slot + "[" + idx + "].type;\n");
            mth.append("  jfvm_arc_inc(jvm, " + slot + "[" + idx + "].obj);\n");
            break;
          case 0x2a:  //aload_0
            idx = 0;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].obj = " + slot + "[" + idx + "].obj;\n");
            mth.append("  stack[stackpos].type = 'L';\n");
            mth.append("  jfvm_arc_inc(jvm, " + slot + "[" + idx + "].obj);\n");
            break;
          case 0x2b:  //aload_1
            idx = 1;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].obj = " + slot + "[" + idx + "].obj;\n");
            mth.append("  stack[stackpos].type = 'L';\n");
            mth.append("  jfvm_arc_inc(jvm, " + slot + "[" + idx + "].obj);\n");
            break;
          case 0x2c:  //aload_2
            idx = 2;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].obj = " + slot + "[" + idx + "].obj;\n");
            mth.append("  stack[stackpos].type = 'L';\n");
            mth.append("  jfvm_arc_inc(jvm, " + slot + "[" + idx + "].obj);\n");
            break;
          case 0x2d:  //aload_3
            idx = 3;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].obj = " + slot + "[" + idx + "].obj;\n");
            mth.append("  stack[stackpos].type = 'L';\n");
            mth.append("  jfvm_arc_inc(jvm, " + slot + "[" + idx + "].obj);\n");
            break;
          case 0xbd:  //anewarray
            //count -> arrayref
            idx = readShort() & 0xffff;  //object name
            popInt(0);  //count
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].obj = jfvm_anewarray(jvm, " + quoteString(cls.getConstString(idx)) + ", temp[0].i32);\n");
            mth.append("  stack[stackpos].type = 'L';\n");
            break;
          case 0xb0:  //areturn
            mth.append("  goto method_done;\n");
            break;
          case 0xbe:  //arraylength
            //arrayref -> length
            mth.append("  if (stack[stackpos].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  len = stack[stackpos].obj->array->length;\n");
            mth.append("  jfvm_arc_release(jvm, &stack[stackpos]);\n");
            mth.append("  stack[stackpos].i32 = len;\n");
            mth.append("  stack[stackpos].type = 'I';\n");
            break;
          case 0x3a:  //astore
            //objectref -> local [idx]
            if (wide) {
              idx = readShort() & 0xffff;
            } else {
              idx = readByte() & 0xff;
            }
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  if (" + slot + "[" + idx + "].type == 'L') jfvm_arc_release(jvm, &" + slot + "[" + idx + "]);\n");
            mth.append("  " + slot + "[" + idx + "].obj = stack[stackpos].obj;\n");
            mth.append("  " + slot + "[" + idx + "].type = stack[stackpos].type;\n");
            mth.append("  stack[stackpos].type = 0;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x4b:  //astore_0
            //objectref -> local [0]
            idx = 0;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  if (" + slot + "[" + idx + "].type == 'L') jfvm_arc_release(jvm, &" + slot + "[" + idx + "]);\n");
            mth.append("  " + slot + "[" + idx + "].obj = stack[stackpos].obj;\n");
            mth.append("  " + slot + "[" + idx + "].type = stack[stackpos].type;\n");
            mth.append("  stack[stackpos].type = 0;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x4c:  //astore_1
            //objectref -> local [1]
            idx = 1;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  if (" + slot + "[" + idx + "].type == 'L') jfvm_arc_release(jvm, &" + slot + "[" + idx + "]);\n");
            mth.append("  " + slot + "[" + idx + "].obj = stack[stackpos].obj;\n");
            mth.append("  " + slot + "[" + idx + "].type = stack[stackpos].type;\n");
            mth.append("  stack[stackpos].type = 0;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x4d:  //astore_2
            //objectref -> local [2]
            idx = 2;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  if (" + slot + "[" + idx + "].type == 'L') jfvm_arc_release(jvm, &" + slot + "[" + idx + "]);\n");
            mth.append("  " + slot + "[" + idx + "].obj = stack[stackpos].obj;\n");
            mth.append("  " + slot + "[" + idx + "].type = stack[stackpos].type;\n");
            mth.append("  stack[stackpos].type = 0;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x4e:  //astore_3
            //objectref -> local [3]
            idx = 3;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  if (" + slot + "[" + idx + "].type == 'L') jfvm_arc_release(jvm, &" + slot + "[" + idx + "]);\n");
            mth.append("  " + slot + "[" + idx + "].obj = stack[stackpos].obj;\n");
            mth.append("  " + slot + "[" + idx + "].type = stack[stackpos].type;\n");
            mth.append("  stack[stackpos].type = 0;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xbf:  //athrow
            mth.append("  stack[stackpos].type = 0;\n");  //hide object from auto release
            mth.append("  jfvm_throw_exception(jvm, stack[stackpos].obj);\n");
            break;
          case 0x33:  //baload (byte or boolean)
            // arrayref, index -> value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[2].i32 = temp[1].obj->array->ai8[temp[0].i32];\n");
            pushByte(2);
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x54:  //bastore
            //arrayref, index, value ->
            popByte(2);  //value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[1].obj->array->ai8[temp[0].i32] = temp[2].i8;\n");
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x10:  //bipush
            val = readByte();
            pushiInt(val);
            break;
          case 0x34:  //caload
            // arrayref, index -> value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[2].i32 = temp[1].obj->array->ai16[temp[0].i32];\n");
            pushChar(2);
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x55:  //castore
            popChar(2);  //value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[1].obj->array->ai16[temp[0].i32] = temp[2].i16;\n");
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0xc0:  //checkcast
            //objref (obj S) -> objref
            idx = readShort() & 0xffff;  //-> ConstClass, Array, or ConstInterfaceRef (type T)
            //throw exception ClassCastException if S is NOT of type T
            //ignore if S is null (do NOT throw NPE)
            //do not change the stack
            constRef = cls.ConstList[idx];
            mth.append("  if (stack[stackpos].obj != NULL) {\n");
              if (constRef instanceof ConstClass) {
                classRef = (ConstClass)cls.ConstList[idx];
                scls = cls.getConstString(classRef.idx);
                if (scls.startsWith("[")) {
                  //arrays extend Object
                  scls = "java/lang/Object";
                }
                xcls = clspool.getClass(scls);
                mth.append("  jfvm_checkcast_class(jvm," + addClass(xcls.name) + ",&stack[stackpos]);\n");
              } else if (constRef instanceof ConstInterfaceRef) {
                ifaceRef = (ConstInterfaceRef)cls.ConstList[idx];
                scls = cls.getConstString(ifaceRef.cls_idx);
                xcls = clspool.getClass(scls);
                mth.append("  jfvm_checkcast_interface(jvm," + addClass(xcls.name) + ",&stack[stackpos]);\n");
              } else {
                //must be array type
                classRef = (ConstClass)cls.ConstList[idx];
                scls = cls.getConstString(classRef.idx);
                xcls = clspool.getClass(scls);
                mth.append("  jfvm_checkcast_array(jvm," + addClass(xcls.name) + ",&stack[stackpos]);\n");
              }
            mth.append("}\n");
            break;
          case 0x90:  //d2f
            mth.append("  stack[stackpos].f32 = stack[stackpos].f64;\n");
            mth.append("  stack[stackpos].type = 'F';\n");
            break;
          case 0x8e:  //d2i
            mth.append("  stack[stackpos].i32 = stack[stackpos].f64;\n");
            mth.append("  stack[stackpos].type = 'I';\n");
            break;
          case 0x8f:  //d2l
            mth.append("  stack[stackpos].i64 = stack[stackpos].f64;\n");
            mth.append("  stack[stackpos].type = 'J';\n");
            break;
          case 0x63:  //dadd
            mth.append("  stack[stackpos-1].f64 += stack[stackpos].f64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x31:  //daload
            // arrayref, index -> value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[2].f64 = temp[1].obj->array->af64[temp[0].i32];\n");
            pushInt(2);
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x52:  //dastore
            popByte(2);  //value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[1].obj->array->af64[temp[0].i32] = temp[2].f64;\n");
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x98:  //dcmpg
          case 0x97:  //dcmpl
            // value1, value2 -> result (1=value1 > value2; 0=equal -1=value1 < value2)
            popDouble(0);
            popDouble(1);
            mth.append("  if (temp[0].f64 == temp[1].f64) {\n");
              pushiInt(0);
            mth.append("  } else if (temp[0].f64 < temp[1].f64) {\n");
              pushiInt(-1);
            mth.append("  } else if (temp[0].f64 > temp[1].f64) {\n");
              pushiInt(1);
            mth.append("  } else {\n");
              //unordered error (NaN) dcmpg=1 dcmpl=-1
              if (bytecode == 0x98) {
                pushiInt(1);
              } else {
                pushiInt(-1);
              }
            mth.append("  }\n");
            break;
          case 0x0e:  //dconst_0
            pushiDouble(0.0);
            break;
          case 0x0f:  //dconst_1
            pushiDouble(1.0);
            break;
          case 0x6f:  //ddiv
            mth.append("  if (stack[stackpos].f64 == 0.0) jfvm_throw_divbyzero(jvm);\n");
            mth.append("  stack[stackpos-1].f64 /= stack[stackpos].f64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x18:  //dload
            if (wide) {
              idx = readShort() & 0xffff;
            } else {
              idx = readByte() & 0xff;
            }
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].f64 = " + slot + "[" + idx + "].f64;\n");
            mth.append("  stack[stackpos].type = 'D';\n");
            break;
          case 0x26:  //dload_0
            idx = 0;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].f64 = " + slot + "[" + idx + "].f64;\n");
            mth.append("  stack[stackpos].type = 'D';\n");
            break;
          case 0x27:  //dload_1
            idx = 1;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].f64 = " + slot + "[" + idx + "].f64;\n");
            mth.append("  stack[stackpos].type = 'D';\n");
            break;
          case 0x28:  //dload_2
            idx = 2;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].f64 = " + slot + "[" + idx + "].f64;\n");
            mth.append("  stack[stackpos].type = 'D';\n");
            break;
          case 0x29:  //dload_3
            idx = 3;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].f64 = " + slot + "[" + idx + "].f64;\n");
            mth.append("  stack[stackpos].type = 'D';\n");
            break;
          case 0x6b:  //dmul
            mth.append("  stack[stackpos-1].f64 *= stack[stackpos].f64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x77:  //dneg
            mth.append("  stack[stackpos].f64 *= -1;\n");
            break;
          case 0x73:  //drem
            // value1, value2 -> result
            mth.append("  stack[stackpos-1].f64 = remainder(stack[stackpos-1].f64, stack[stackpos].f64);\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xaf:  //dreturn
            mth.append("  goto method_done;\n");
            break;
          case 0x39:  //dstore
            if (wide) {
              idx = readShort() & 0xffff;
            } else {
              idx = readByte() & 0xff;
            }
            popAny(0);
            putAny(idx, 0);
            break;
          case 0x47:  //dstore_0
            popAny(0);
            putAny(0, 0);
            break;
          case 0x48:  //dstore_1
            popAny(0);
            putAny(1, 0);
            break;
          case 0x49:  //dstore_2
            popAny(0);
            putAny(2, 0);
            break;
          case 0x4a:  //dstore_3
            popAny(0);
            putAny(3, 0);
            break;
          case 0x67:  //dsub
            mth.append("  stack[stackpos-1].f64 -= stack[stackpos].f64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x59:  //dup
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i64 = stack[stackpos-1].i64;\n");
            mth.append("  stack[stackpos].type = stack[stackpos-1].type;\n");
            mth.append("  if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            break;
          case 0x5c:  //dup2
            //2 forms
            mth.append("  if (stack[stackpos].type == 'J' || stack[stackpos].type == 'D') {\n");
            mth.append("    stackpos++;\n");
            mth.append("    stack[stackpos].i64 = stack[stackpos-1].i64;\n");
            mth.append("    stack[stackpos].type = stack[stackpos-1].type;\n");
            mth.append("    if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            mth.append("  } else {\n");
            mth.append("    stackpos++;\n");
            mth.append("    stack[stackpos].i64 = stack[stackpos-2].i64;\n");
            mth.append("    stack[stackpos].type = stack[stackpos-2].type;\n");
            mth.append("    if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            mth.append("    stackpos++;\n");
            mth.append("    stack[stackpos].i64 = stack[stackpos-2].i64;\n");
            mth.append("    stack[stackpos].type = stack[stackpos-2].type;\n");
            mth.append("    if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            mth.append("  }\n");
            break;
          case 0x5a:  //dup_x1
            //before : obj2, obj1
            //after : *obj1*, obj2, obj1
            insert(-2);
            mth.append("  stack[stackpos-2].i64 = stack[stackpos].i64;\n");
            mth.append("  stack[stackpos-2].type = stack[stackpos].type;\n");
            mth.append("  if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            break;
          case 0x5d:  //dup2_x1  (slot size makes no difference)
            //2 forms
            mth.append("  if (stack[stackpos].type == 'J' || stack[stackpos].type == 'D') {\n");
            //before : obj2, obj1
            //after : *obj1*, obj2, obj1
            insert(-2);
            mth.append("    stack[stackpos-2].i64 = stack[stackpos].i64;\n");
            mth.append("    stack[stackpos-2].type = stack[stackpos].type;\n");
            mth.append("    if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            mth.append("  } else {\n");
            //before : obj3, obj2, obj1
            //after : *obj2*, *obj1*, obj3, obj2, obj1
            insert2(-3);
            mth.append("    stack[stackpos-4].i64 = stack[stackpos-1].i64;\n");
            mth.append("    stack[stackpos-4].type = stack[stackpos-1].type;\n");
            mth.append("    if (stack[stackpos-1].type == 'L') jfvm_arc_inc(jvm, stack[stackpos-1].obj);\n");
            mth.append("    stack[stackpos-3].i64 = stack[stackpos].i64;\n");
            mth.append("    stack[stackpos-3].type = stack[stackpos].type;\n");
            mth.append("    if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            mth.append("  }\n");
            break;
          case 0x5b:  //dup_x2
            //before : obj3, obj2, obj1
            //after : *obj1*, obj3, obj2, obj1
            insert(-3);
            mth.append("  stack[stackpos-3].i64 = stack[stackpos].i64;\n");
            mth.append("  stack[stackpos-3].type = stack[stackpos].type;\n");
            mth.append("  if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            break;
          case 0x5e:  //dup2_x2 (slot size makes no difference)
            //4 forms
            mth.append("  if ((stack[stackpos].type == 'J' || stack[stackpos].type == 'D') && (stack[stackpos-1].type == 'J' || stack[stackpos-1].type == 'D')) {\n");
            //form 4
            //before : obj2, obj1
            //after : *obj1*, obj2, obj1
            insert(-2);
            mth.append("    stack[stackpos-2].i64 = stack[stackpos].i64;\n");
            mth.append("    stack[stackpos-2].type = stack[stackpos].type;\n");
            mth.append("    jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            mth.append("  } else if ((stack[stackpos].type != 'J' && stack[stackpos].type != 'D') && (stack[stackpos-1].type != 'J' && stack[stackpos-1].type != 'D') && (stack[stackpos-2].type == 'J' || stack[stackpos-2].type == 'D')) {\n");
            //form 3
            //before : obj3, obj2, obj1
            //after : *obj2*, *obj1*, obj3, obj2, obj1
            insert2(-3);
            mth.append("    stack[stackpos-3].i64 = stack[stackpos].i64;\n");
            mth.append("    stack[stackpos-3].type = stack[stackpos].type;\n");
            mth.append("    if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            mth.append("    stack[stackpos-4].i64 = stack[stackpos-1].i64;\n");
            mth.append("    stack[stackpos-4].type = stack[stackpos-1].type;\n");
            mth.append("    if (stack[stackpos-1].type == 'L') jfvm_arc_inc(jvm, stack[stackpos-1].obj);\n");
            mth.append("  } else if ((stack[stackpos].type == 'J' || stack[stackpos].type == 'D') && (stack[stackpos-1].type != 'J' && stack[stackpos-1].type != 'D') && (stack[stackpos-2].type != 'J' && stack[stackpos-2].type != 'D')) {\n");
            //form 2
            //before : obj3, obj2, obj1
            //after : *obj1*, obj3, obj2, obj1
            insert(-3);
            mth.append("    stack[stackpos-4].i64 = stack[stackpos].i64;\n");
            mth.append("    stack[stackpos-4].type = stack[stackpos].type;\n");
            mth.append("    if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            mth.append("  } else if ((stack[stackpos].type != 'J' && stack[stackpos].type != 'D') && (stack[stackpos-1].type != 'J' && stack[stackpos-1].type != 'D') && (stack[stackpos-2].type != 'J' && stack[stackpos-2].type != 'D') && (stack[stackpos-3].type != 'J' && stack[stackpos-3].type != 'D')) {\n");
            //form 1
            //before : obj4, obj3, obj2, obj1
            //after : *obj2*, *obj1*, obj4, obj3, obj2, obj1
            insert(-4);
            mth.append("    stack[stackpos-4].i64 = stack[stackpos].i64;\n");
            mth.append("    stack[stackpos-4].type = stack[stackpos].type;\n");
            mth.append("    if (stack[stackpos].type == 'L') jfvm_arc_inc(jvm, stack[stackpos].obj);\n");
            mth.append("    stack[stackpos-5].i64 = stack[stackpos-1].i64;\n");
            mth.append("    stack[stackpos-5].type = stack[stackpos-1].type;\n");
            mth.append("    if (stack[stackpos-1].type == 'L') jfvm_arc_inc(jvm, stack[stackpos-1].obj);\n");
            mth.append("  }\n");
            break;
          case 0x8d:  //f2d
            mth.append("  stack[stackpos].f64 = stack[stackpos].f32;\n");
            mth.append("  stack[stackpos].type = 'D';\n");
            break;
          case 0x8b:  //f2i
            mth.append("  stack[stackpos].i32 = stack[stackpos].f32;\n");
            mth.append("  stack[stackpos].type = 'I';\n");
            break;
          case 0x8c:  //f2l
            mth.append("  stack[stackpos].i64 = stack[stackpos].f32;\n");
            mth.append("  stack[stackpos].type = 'J';\n");
            break;
          case 0x62:  //fadd
            mth.append("  stack[stackpos-1].f32 += stack[stackpos].f32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x30:  //faload
            // arrayref, index -> value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[2].f32 = temp[1].obj->array->af32[temp[0].i32];\n");
            pushInt(2);
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x51:  //fastore
            popByte(2);  //value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[1].obj->array->af32[temp[0].i32] = temp[2].f32;\n");
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x96:  //fcmpg
          case 0x95:  //fcmpl
            // value1, value2 -> result (1=value1 > value2; 0=equal -1=value1 < value2)
            popDouble(0);
            popDouble(1);
            mth.append("  if (temp[0].f32 == temp[1].f32) {\n");
              pushiInt(0);
            mth.append("  } else if (temp[0].f32 < temp[1].f32) {\n");
              pushiInt(-1);
            mth.append("  } else if (temp[0].f32 > temp[1].f32) {\n");
              pushiInt(1);
            mth.append("  } else {\n");
              //unordered error (NaN) dcmpg=1 dcmpl=-1
              if (bytecode == 0x98) {
                pushiInt(1);
              } else {
                pushiInt(-1);
              }
            mth.append("  }\n");
            break;
          case 0x0b:  //fconst_0
            pushiFloat(0.0f);
            break;
          case 0x0c:  //fconst_1
            pushiFloat(1.0f);
            break;
          case 0x0d:  //fconst_2
            pushiFloat(2.0f);
            break;
          case 0x6e:  //fdiv
            mth.append("  if (stack[stackpos].f32 == 0.0) jfvm_throw_divbyzero(jvm);\n");
            mth.append("  stack[stackpos-1].f32 /= stack[stackpos].f32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x17:  //fload
            if (wide) {
              idx = readShort() & 0xffff;
            } else {
              idx = readByte() & 0xff;
            }
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].f32 = " + slot + "[" + idx + "].f32;\n");
            mth.append("  stack[stackpos].type = 'F';\n");
            break;
          case 0x22:  //fload_0
            idx = 0;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].f32 = " + slot + "[" + idx + "].f32;\n");
            mth.append("  stack[stackpos].type = 'F';\n");
            break;
          case 0x23:  //fload_1
            idx = 1;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].f32 = " + slot + "[" + idx + "].f32;\n");
            mth.append("  stack[stackpos].type = 'F';\n");
            break;
          case 0x24:  //fload_2
            idx = 2;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].f32 = " + slot + "[" + idx + "].f32;\n");
            mth.append("  stack[stackpos].type = 'F';\n");
            break;
          case 0x25:  //fload_3
            idx = 3;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].f32 = " + slot + "[" + idx + "].f32;\n");
            mth.append("  stack[stackpos].type = 'F';\n");
            break;
          case 0x6a:  //fmul
            mth.append("  stack[stackpos-1].f32 *= stack[stackpos].f32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x76:  //fneg
            mth.append("  stack[stackpos].f32 *= -1;\n");
            break;
          case 0x72:  //frem
            // value1, value2 -> result
            mth.append("  stack[stackpos-1].f32 = remainder(stack[stackpos-1].f32, stack[stackpos].f32);\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xae:  //freturn
            mth.append("  goto method_done;\n");
            break;
          case 0x38:  //fstore
            if (wide) {
              idx = readShort() & 0xffff;
            } else {
              idx = readByte() & 0xff;
            }
            popAny(0);
            putAny(idx, 0);
            break;
          case 0x43:  //fstore_0
            popAny(0);
            putAny(0, 0);
            break;
          case 0x44:  //fstore_1
            popAny(0);
            putAny(1, 0);
            break;
          case 0x45:  //fstore_2
            popAny(0);
            putAny(2, 0);
            break;
          case 0x46:  //fstore_3
            popAny(0);
            putAny(3, 0);
            break;
          case 0x66:  //fsub
            mth.append("  stack[stackpos-1].f32 -= stack[stackpos].f32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xb4:  //getfield
            //objectref -> value
            idx = readShort() & 0xffff;
            fieldRef = (ConstFieldRef)cls.ConstList[idx];
            scls = cls.getConstString(fieldRef.cls_idx);
            sfield = cls.getConstName(fieldRef.name_type_idx);
            type = cls.getConstType(fieldRef.name_type_idx);
            xcls = clspool.getClass(scls);
            xfield = xcls.getField(sfield + "$" + type);
            popObject(0);
            mth.append("  jfvm_getfield(jvm," + addClass(scls) + ",'" + type.charAt(0) + "'," + xfield.objidx + ",&temp[0],&temp[1]);\n");
            pushAny(1);
            mth.append("  jfvm_arc_release(jvm, &temp[0]);\n");
            break;
          case 0xb2:  //getstatic
            // -> value
            idx = readShort() & 0xffff;
            fieldRef = (ConstFieldRef)cls.ConstList[idx];
            scls = cls.getConstString(fieldRef.cls_idx);
            sfield = cls.getConstName(fieldRef.name_type_idx);
            type = cls.getConstType(fieldRef.name_type_idx);
            xcls = clspool.getClass(scls);
            xfield = xcls.getField(sfield + "$" + type);
            mth.append("  jfvm_getstatic(jvm," + addClass(xcls.name) + ",'" + type.charAt(0) + "'," + xfield.clsidx + ",&temp[0]);\n");
            pushAny(0);
            break;
          case 0xa7:  //goto
            idx = readShort();  //signed offset
            mth.append("  goto pc_" + (opc+idx) + ";\n");
            break;
          case 0xc8:  //goto_w
            idx = readInt();  //signed offset
            mth.append("  goto pc_" + (opc+idx) + ";\n");
            break;
          case 0x91:  //i2b
            mth.append("  stack[stackpos].i32 &= 0xff;\n");
            break;
          case 0x92:  //i2c
          case 0x93:  //i2s
            mth.append("  stack[stackpos].i32 &= 0xffff;\n");
            break;
          case 0x87:  //i2d
            mth.append("  stack[stackpos].f64 = stack[stackpos].i32;\n");
            break;
          case 0x86:  //i2f
            mth.append("  stack[stackpos].f32 = stack[stackpos].i32;\n");
            break;
          case 0x85:  //i2l
            mth.append("  stack[stackpos].i64 = stack[stackpos].i32;\n");
            break;
          case 0x60:  //iadd
            mth.append("  stack[stackpos-1].i32 += stack[stackpos].i32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x2e:  //iaload
            // arrayref, index -> value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[2].i32 = temp[1].obj->array->ai32[temp[0].i32];\n");
            pushInt(2);
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x7e:  //iand
            //value1, value2 -> result
            mth.append("  stack[stackpos-1].i32 &= stack[stackpos].i32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x4f:  //iastore
            popByte(2);  //value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[1].obj->array->ai32[temp[0].i32] = temp[2].i32;\n");
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x02:  //iconst_m1
            pushiInt(-1);
            break;
          case 0x03:  //iconst_0
            pushiInt(0);
            break;
          case 0x04:  //iconst_1
            pushiInt(1);
            break;
          case 0x05:  //iconst_2
            pushiInt(2);
            break;
          case 0x06:  //iconst_3
            pushiInt(3);
            break;
          case 0x07:  //iconst_4
            pushiInt(4);
            break;
          case 0x08:  //iconst_5
            pushiInt(5);
            break;
          case 0x6c:  //idiv
            mth.append("  if (stack[stackpos].i32 == 0.0) jfvm_throw_divbyzero(jvm);\n");
            mth.append("  stack[stackpos-1].i32 /= stack[stackpos].i32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xa5:  //if_acmpeq
            idx = readShort();  //signed offset
            popObject(0);
            popObject(1);
            //NOTE : jfvm_arc_release() is called first so it must not change .obj
            mth.append("  jfvm_arc_release(jvm, &temp[0]);\n");
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            mth.append("  if (temp[0].obj == temp[1].obj) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0xa6:  //if_acmpne
            idx = readShort();  //signed offset
            popObject(0);
            popObject(1);
            //NOTE : jfvm_arc_release() is called first so it must not change .obj
            mth.append("  jfvm_arc_release(jvm, &temp[0]);\n");
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            mth.append("  if (temp[0].obj != temp[1].obj) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0x9f:  //if_icmpeq
            idx = readShort();  //signed offset
            popInt(1);
            popInt(0);
            mth.append("  if (temp[0].i32 == temp[1].i32) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0xa2:  //if_icmpge
            idx = readShort();  //signed offset
            popInt(1);
            popInt(0);
            mth.append("  if (temp[0].i32 >= temp[1].i32) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0xa3:  //if_icmpgt
            idx = readShort();  //signed offset
            popInt(1);
            popInt(0);
            mth.append("  if (temp[0].i32 > temp[1].i32) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0xa4:  //if_icmple
            idx = readShort();  //signed offset
            popInt(1);
            popInt(0);
            mth.append("  if (temp[0].i32 <= temp[1].i32) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0xa1:  //if_icmplt
            idx = readShort();  //signed offset
            popInt(1);
            popInt(0);
            mth.append("  if (temp[0].i32 < temp[1].i32) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0xa0:  //if_icmpne
            idx = readShort();  //signed offset
            popInt(1);
            popInt(0);
            mth.append("  if (temp[0].i32 != temp[1].i32) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0x99:  //ifeq
            idx = readShort();  //signed offset
            popInt(0);
            mth.append("  if (temp[0].i32 == 0) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0x9c:  //ifge
            idx = readShort();  //signed offset
            popInt(0);
            mth.append("  if (temp[0].i32 >= 0) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0x9d:  //ifgt
            idx = readShort();  //signed offset
            popInt(0);
            mth.append("  if (temp[0].i32 > 0) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0x9e:  //ifle
            idx = readShort();  //signed offset
            popInt(0);
            mth.append("  if (temp[0].i32 <= 0) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0x9b:  //iflt
            idx = readShort();  //signed offset
            popInt(0);
            mth.append("  if (temp[0].i32 < 0) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0x9a:  //ifne
            idx = readShort();  //signed offset
            popInt(0);
            mth.append("  if (temp[0].i32 != 0) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0xc7:  //ifnotnull
            idx = readShort();  //signed offset
            popObject(0);
            //NOTE : jfvm_arc_release() is called first so it must not change .obj
            mth.append("  jfvm_arc_release(jvm, &temp[0]);\n");
            mth.append("  if (temp[0].obj != NULL) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0xc6:  //ifnull
            idx = readShort();  //signed offset
            popObject(0);
            mth.append("  jfvm_arc_release(jvm, &temp[0]);\n");
            mth.append("  if (temp[0].obj == NULL) {\n");
            mth.append("    goto " + "pc_" + (opc+idx) + ";\n");
            mth.append("  }\n");
            break;
          case 0x84:  //iinc
            if (wide) {
              idx = readShort() & 0xffff;
              val = readShort();
            } else {
              idx = readByte() & 0xff;
              val = readByte();
            }
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  " + slot + "[" + idx + "].i32 += " + val + ";\n");
            break;
          case 0x15:  //iload
            if (wide) {
              idx = readShort() & 0xffff;
            } else {
              idx = readByte() & 0xff;
            }
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i32 = " + slot + "[" + idx + "].i32;\n");
            mth.append("  stack[stackpos].type = 'I';\n");
            break;
          case 0x1a:  //iload_0
            idx = 0;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i32 = " + slot + "[" + idx + "].i32;\n");
            mth.append("  stack[stackpos].type = 'I';\n");
            break;
          case 0x1b:  //iload_1
            idx = 1;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i32 = " + slot + "[" + idx + "].i32;\n");
            mth.append("  stack[stackpos].type = 'I';\n");
            break;
          case 0x1c:  //iload_2
            idx = 2;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i32 = " + slot + "[" + idx + "].i32;\n");
            mth.append("  stack[stackpos].type = 'I';\n");
            break;
          case 0x1d:  //iload_3
            idx = 3;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i32 = " + slot + "[" + idx + "].i32;\n");
            mth.append("  stack[stackpos].type = 'I';\n");
            break;
          case 0x68:  //imul
            mth.append("  stack[stackpos-1].i32 *= stack[stackpos].i32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x74:  //ineg
            mth.append("  stack[stackpos].i32 *= -1;\n");
            break;
          case 0xc1:  //instanceof
            //objectref -> boolean
            idx = readShort() & 0xffff;
            popObject(0);
            mth.append("  jfvm_instanceof(jvm," + quoteString(cls.getConstString(idx)) + ",temp[0])\n");  //will release objectref
            pushBoolean(0);
            break;
          case 0xba:  //invokedynamic
            idx = readShort() & 0xffff;
            readShort();  //0,0
            ConstDynamic dyn = (ConstDynamic)cls.ConstList[idx];
            ConstNameType dynnt = (ConstNameType)cls.ConstList[dyn.name_and_type_index];
            String dyn_name = cls.getConstString(dynnt.name_idx);  //method name in interface
            String dyn_desc = cls.getConstString(dynnt.desc_idx);  //()Lclass;
            String dyn_cls = dyn_desc.substring(3, dyn_desc.length() - 1);
            xcls = clspool.getClass(dyn_cls);
            //lamba interfaces ALWAYS have just one method
            xmethod = xcls.MethodList[0];  //xcls.getMethod(dyn_name + dyn_desc);  //dyn_desc is wrong!?!
            mth.append("//dyn_name=" + dyn_name + ",dyn_desc=" + dyn_desc + ",bootstrap_idx=" + dyn.bootstrap_method_attr_index + "\n");
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].obj = jfvm_create_lambda(jvm," + addClass(dyn_cls) + "," + xmethod.objidx + ", &lambda_" + dyn.bootstrap_method_attr_index + ");\n");
            mth.append("  stack[stackpos].type = 'L';\n");
            break;
          case 0xb9:  //invokeinterface
            idx = readShort() & 0xffff;
            val = readByte(); //count
            readByte();  //0
            if (cls.ConstList[idx] instanceof ConstInterfaceRef) {
              System.out.println("invokeinterface with InterfaceRef !!!");
              ifaceRef = (ConstInterfaceRef)cls.ConstList[idx];
              scls = cls.getConstString(ifaceRef.cls_idx);
              xcls = clspool.getClass(scls);
              smethod = cls.getConstName(ifaceRef.name_type_idx);
              type = cls.getConstType(ifaceRef.name_type_idx);
            } else {
              methodRef = (ConstMethodRef)cls.ConstList[idx];
              scls = cls.getConstString(methodRef.cls_idx);
              xcls = clspool.getClass(scls);
              smethod = cls.getConstName(methodRef.name_type_idx);
              type = cls.getConstType(methodRef.name_type_idx);
            }
            xmethod = xcls.getMethod(smethod + type);
            cnt = xmethod.getArgsCount();
            cnt--;
            mth.append("  if (stack[stackpos" + getStackOffsetToArgs(cnt) + "].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  //" + xcls.name + "." + smethod + type + "\n");
            mth.append("  jfvm_invokevirtual(jvm," + addClass(xcls.name) + "," + xcls.getMethodObjOffset(smethod + type) + ", &stack[stackpos" + getStackOffsetToArgs(cnt) + "]);\n");
            cnt = xmethod.getArgsCount();
            if (!xmethod.isVoid()) cnt--;  //do not remove return value (cnt may be negative)
            if (cnt != 0) mth.append("  stackpos" + getStackCountToArgs(cnt) + ";\n");
            break;
          case 0xb7:  //invokespecial (basically a non-virtual method call)
            idx = readShort() & 0xffff;
            methodRef = (ConstMethodRef)cls.ConstList[idx];
            scls = cls.getConstString(methodRef.cls_idx);
            xcls = clspool.getClass(scls);
            smethod = cls.getConstName(methodRef.name_type_idx);
            type = cls.getConstType(methodRef.name_type_idx);
            xmethod = xcls.getMethod(smethod + type);
            cnt = xmethod.getArgsCount();
            cnt--;
            mth.append("  if (stack[stackpos" + getStackOffsetToArgs(cnt) + "].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  //" + xcls.name + "." + smethod + type + "\n");
            mth.append("  jfvm_invokespecial(jvm," + addClass(xcls.name) + "," + xcls.getMethodClsOffset(smethod + type) + ", &stack[stackpos" + getStackOffsetToArgs(cnt) + "]);\n");
            cnt = xmethod.getArgsCount();
            if (!xmethod.isVoid()) cnt--;  //do not remove return value (cnt may be negative)
            if (cnt != 0) mth.append("  stackpos" + getStackCountToArgs(cnt) + ";\n");
            break;
          case 0xb8:  //invokestatic
            idx = readShort() & 0xffff;
            methodRef = (ConstMethodRef)cls.ConstList[idx];
            scls = cls.getConstString(methodRef.cls_idx);
            xcls = clspool.getClass(scls);
            smethod = cls.getConstName(methodRef.name_type_idx);
            type = cls.getConstType(methodRef.name_type_idx);
            xmethod = xcls.getMethod(smethod + type);
            cnt = xmethod.getArgsCount();
            cnt--;
            mth.append("  //" + xcls.name + "." + smethod + type + "\n");
            mth.append("  jfvm_invokestatic(jvm," + addClass(xcls.name) + "," + xcls.getMethodClsOffset(smethod + type) + ", &stack[stackpos" + getStackOffsetToArgs(cnt) + "]);\n");
            cnt = xmethod.getArgsCount();
            if (!xmethod.isVoid()) cnt--;  //do not remove return value (cnt may be negative)
            if (cnt != 0) mth.append("  stackpos" + getStackCountToArgs(cnt) + ";\n");
            break;
          case 0xb6:  //invokevirtual
            idx = readShort() & 0xffff;
            methodRef = (ConstMethodRef)cls.ConstList[idx];
            scls = cls.getConstString(methodRef.cls_idx);
            smethod = cls.getConstName(methodRef.name_type_idx);
            type = cls.getConstType(methodRef.name_type_idx);
            if (scls.startsWith("[")) {
              //arrays extend Object
              scls = "java/lang/Object";
            }
            xcls = clspool.getClass(scls);
            xmethod = xcls.getMethod(smethod + type);
            cnt = xmethod.getArgsCount();
            cnt--;
            mth.append("  if (stack[stackpos" + getStackOffsetToArgs(cnt) + "].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  //" + xcls.name + "." + smethod + type + "\n");
            mth.append("  jfvm_invokevirtual(jvm," + addClass(xcls.name) + "," + xcls.getMethodObjOffset(smethod + type) + ", &stack[stackpos" + getStackOffsetToArgs(cnt) + "]);\n");
            cnt = xmethod.getArgsCount();
            if (!xmethod.isVoid()) cnt--;  //do not remove return value (cnt may be negative)
            if (cnt != 0) mth.append("  stackpos" + getStackCountToArgs(cnt) + ";\n");
            break;
          case 0x80:  //ior
            //value1, value2 -> result
            mth.append("  stack[stackpos-1].i32 |= stack[stackpos].i32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x70:  //irem
            mth.append("  stack[stackpos-1].i32 = stack[stackpos-1].i32 % stack[stackpos].i32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xac:  //ireturn
            mth.append("  goto method_done;\n");
            break;
          case 0x78:  //ishl
            popInt(0);
            mth.append("  stack[stackpos].i32 <<= temp[0].i8;\n");
            break;
          case 0x7a:  //ishr
            popInt(0);
            mth.append("  stack[stackpos].i32 >>= temp[0].i8;\n");
            break;
          case 0x36:  //istore
            if (wide) {
              idx = readShort() & 0xffff;
            } else {
              idx = readByte() & 0xff;
            }
            popInt(0);
            putInt(idx, 0);
            break;
          case 0x3b:  //istore_0
            popInt(0);
            putInt(0, 0);
            break;
          case 0x3c:  //istore_1
            popInt(0);
            putInt(1, 0);
            break;
          case 0x3d:  //istore_2
            popInt(0);
            putInt(2, 0);
            break;
          case 0x3e:  //istore_3
            popInt(0);
            putInt(3, 0);
            break;
          case 0x64:  //isub
            mth.append("  stack[stackpos-1].i32 -= stack[stackpos].i32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x7c:  //iushr
            popInt(0);
            mth.append("  stack[stackpos].u32 >>= temp[0].i8;\n");
            break;
          case 0x82:  //ixor
            mth.append("  stack[stackpos-1].i32 ^= stack[stackpos].i32;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xa8:  //jsr
            idx = readShort();  //signed offset
            val = jsridx++;
            pushiInt(val);  //the returnAddress is considered an object, only astore... can work on it
            mth.append("  goto pc_" + (opc+idx) + ";\n");
            mth.append("return_" + val + ":\n");
            break;
          case 0xc9:  //jsr_w
            idx = readInt();  //signed offset
            val = jsridx++;
            pushiInt(val);  //the returnAddress is considered an object, only astore... can work on it
            mth.append("  goto pc_" + (opc+idx) + ";\n");
            mth.append("return_" + val + ":\n");
            break;
          case 0x8a:  //l2d
            mth.append("  stack[stackpos].f64 = stack[stackpos].i64;\n");
            mth.append("  stack[stackpos].type = 'D';\n");
            break;
          case 0x89:  //l2f
            mth.append("  stack[stackpos].f32 = stack[stackpos].i64;\n");
            mth.append("  stack[stackpos].type = 'F';\n");
            break;
          case 0x88:  //l2i
            mth.append("  stack[stackpos].type = 'I';\n");
            break;
          case 0x61:  //ladd
            mth.append("  stack[stackpos-1].i64 += stack[stackpos].i64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x2f:  //laload
            // arrayref, index -> value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[2].i64 = temp[1].obj->array->ai64[temp[0].i32];\n");
            pushInt(2);
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x7f:  //land
            mth.append("  stack[stackpos-1].i64 &= stack[stackpos].i64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x50:  //lastore
            popByte(2);  //value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[1].obj->array->ai64[temp[0].i32] = temp[2].i64;\n");
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x94:  //lcmp
            // value1, value2 -> result (1=value1 > value2; 0=equal -1=value1 < value2)
            popLong(0);
            popLong(1);
            mth.append("  if (temp[0].i64 < temp[1].i64) {\n");
              pushiInt(-1);
            mth.append("  } else if (temp[0].i64 > temp[1].i64) {\n");
              pushiInt(1);
            mth.append("  } else {\n");
              pushiInt(0);
            mth.append("  }\n");
            break;
          case 0x09:  //lconst_0
            pushiLong(0);
            break;
          case 0x0a:  //lconst_1
            pushiLong(1);
            break;
          case 0x12:  //ldc
          case 0x13:  //ldc_w
          case 0x14:  //ldc2_w
          {
            if (bytecode == 0x12)
              idx = readByte() & 0xff;
            else
              idx = readShort() & 0xffff;
            Const c = cls.ConstList[idx];
            switch (c.type) {
              case 3:
                ConstInteger ci = (ConstInteger)c;
                pushiInt(ci.val);
                break;
              case 4:
                ConstFloat cf = (ConstFloat)c;
                pushiFloat(cf.val);
                break;
              case 5:
                ConstLong cl = (ConstLong)c;
                pushiLong(cl.val);
                break;
              case 6:
                ConstDouble cd = (ConstDouble)c;
                pushiDouble(cd.val);
                break;
              case 8:
                ConstString cs = (ConstString)c;
                ConstUTF8 cu = (ConstUTF8)cls.ConstList[cs.idx];
                len = cu.str.length();
                mth.append("  stackpos++;\n");
                mth.append("  stack[stackpos].obj = jfvm_new_string(jvm," + quoteString(cu.str) + "," + len + ");\n");
                mth.append("  stack[stackpos].type = 'L';\n");
                break;
            }
            break;
          }
          case 0x6d:  //ldiv
            mth.append("  if (stack[stackpos].i64 == 0) jfvm_throw_divbyzero(jvm);\n");
            mth.append("  stack[stackpos-1].i64 /= stack[stackpos].i64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x16:  //lload
            if (wide) {
              idx = readShort() & 0xffff;
            } else {
              idx = readByte() & 0xff;
            }
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i64 = " + slot + "[" + idx + "].i64;\n");
            mth.append("  stack[stackpos].type = 'J';\n");
            break;
          case 0x1e:  //lload_0
            idx = 0;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i64 = " + slot + "[" + idx + "].i64;\n");
            mth.append("  stack[stackpos].type = 'J';\n");
            break;
          case 0x1f:  //lload_1
            idx = 1;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i64 = " + slot + "[" + idx + "].i64;\n");
            mth.append("  stack[stackpos].type = 'J';\n");
            break;
          case 0x20:  //lload_2
            idx = 2;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i64 = " + slot + "[" + idx + "].i64;\n");
            mth.append("  stack[stackpos].type = 'J';\n");
            break;
          case 0x21:  //lload_3
            idx = 3;
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].i64 = " + slot + "[" + idx + "].i64;\n");
            mth.append("  stack[stackpos].type = 'J';\n");
            break;
          case 0x69:  //lmul
            mth.append("  stack[stackpos-1].i64 *= stack[stackpos].i64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x75:  //lneg
            mth.append("  stack[stackpos].i64 *= -1;\n");
            break;
          case 0xab:  //lookupswitch
            //read padding
            pad = pc & 0x3;
            if (pad > 0) {
              pad = 0x4 - pad;
              for(int a=0;a<pad;a++) {
                readByte();
              }
            }
            defaultOffset = readInt();
            int npairs = readInt();
            popInt(0);  //key
            for(int a=0;a<npairs;a++) {
              int match = readInt();
              int offset = readInt();
              mth.append("  if (" + match + " == temp[0].i32) {\n");
              mth.append("    goto " + "pc_" + (opc + offset) + ";\n");
              mth.append("  }\n");
            }
            mth.append("  goto " + "pc_" + (opc + defaultOffset) + ";\n");
            break;
          case 0x81:  //lor
            mth.append("  stack[stackpos-1].i64 |= stack[stackpos].i64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x71:  //lrem
            mth.append("  stack[stackpos-1].i64 = stack[stackpos-1].i64 % stack[stackpos].i64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xad:  //lreturn
            mth.append("  goto method_done;\n");
            break;
          case 0x79:  //lshl
            popInt(0);
            mth.append("  stack[stackpos].i64 <<= temp[0].i8;\n");
            break;
          case 0x7b:  //lshr
            popInt(0);
            mth.append("  stack[stackpos].i64 >>= temp[0].i8;\n");
            break;
          case 0x37:  //lstore
            if (wide) {
              idx = readShort() & 0xffff;
            } else {
              idx = readByte() & 0xff;
            }
            popLong(0);
            putLong(idx, 0);
            break;
          case 0x3f:  //lstore_0
            popLong(0);
            putLong(0, 0);
            break;
          case 0x40:  //lstore_1
            popLong(0);
            putLong(1, 0);
            break;
          case 0x41:  //lstore_2
            popLong(0);
            putLong(2, 0);
            break;
          case 0x42:  //lstore_3
            popLong(0);
            putLong(3, 0);
            break;
          case 0x65:  //lsub
            mth.append("  stack[stackpos-1].i64 -= stack[stackpos].i64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x7d:  //lushr
            popInt(0);
            mth.append("  stack[stackpos].u64 >>= temp[0].i8;\n");
            break;
          case 0x83:  //lxor
            mth.append("  stack[stackpos-1].i64 ^= stack[stackpos].i64;\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xc2:  //monitorenter
            mth.append("  if (stack[stackpos].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  jfvm_monitor_enter(jvm, stack[stackpos].obj);\n");
            mth.append("  jfvm_arc_release(jvm, &stack[stackpos]);\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xc3:  //monitorexit
            mth.append("  if (stack[stackpos].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  jfvm_monitor_exit(jvm, stack[stackpos].obj);\n");
            mth.append("  jfvm_arc_release(jvm, &stack[stackpos]);\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xc5:  //multianewarray
            idx = readShort() & 0xffff;
            val = readByte();  //dimensions
            classRef = (ConstClass)cls.ConstList[idx];
            scls = cls.getConstString(classRef.idx);
            mth.append("  stack[stackpos-" + (val-1) + "].obj = jfvm_multianewarray(jvm," + val + "," + quoteString(scls) + ",&stack[stackpos-" + (val-1) + "]);\n");
            mth.append("  stackpos -= " + (val-1) + ";\n");
            mth.append("  stack[stackpos].type = 'L';\n");
            break;
          case 0xbb:  //new
            idx = readShort() & 0xffff;
            classRef = (ConstClass)cls.ConstList[idx];
            scls = cls.getConstString(classRef.idx);
            xcls = clspool.getClass(scls);
            mth.append("  stackpos++;\n");
            mth.append("  stack[stackpos].obj = jfvm_new(jvm," + addClass(xcls.name) + ");\n");
            mth.append("  stack[stackpos].type = 'L';\n");
            break;
          case 0xbc:  //newarray (primitive types only)
            //count -> objectref
            val = readByte();  //type
            type = convertType(val);
            mth.append("  stack[stackpos].obj = jfvm_newarray(jvm, '" + type + "', stack[stackpos].i32);\n");
            mth.append("  stack[stackpos].type = 'L';\n");
            break;
          case 0x00:  //nop
            break;
          case 0x57:  //pop
            mth.append("  if (stack[stackpos].type == 'L') jfvm_arc_release(jvm, &stack[stackpos]);\n");
            mth.append("  stackpos--;\n");
            break;
          case 0x58:  //pop2
            mth.append("  type = stack[stackpos].type;\n");
            mth.append("  if (stack[stackpos].type == 'L') jfvm_arc_release(jvm, &stack[stackpos]);\n");
            mth.append("  stackpos--;\n");
            mth.append("  if (type != 'J' && type != 'D') {\n");
            mth.append("    if (stack[stackpos].type == 'L') jfvm_arc_release(jvm, &stack[stackpos]);\n");
            mth.append("    stackpos--;\n");
            mth.append("  }\n");
            break;
          case 0xb5:  //putfield
            //objectref, value ->
            idx = readShort() & 0xffff;
            fieldRef = (ConstFieldRef)cls.ConstList[idx];
            scls = cls.getConstString(fieldRef.cls_idx);
            sfield = cls.getConstName(fieldRef.name_type_idx);
            type = cls.getConstType(fieldRef.name_type_idx);
            xcls = clspool.getClass(scls);
            xfield = xcls.getField(sfield + "$" + type);
            mth.append("  jfvm_putfield(jvm," + addClass(scls) + ",'" + type.charAt(0) + "'," + xfield.objidx + ", &stack[stackpos-1], &stack[stackpos]);\n");
            mth.append("  if (stack[stackpos].type == 'L') jfvm_arc_release(jvm, &stack[stackpos]);\n");
            mth.append("  stackpos--;\n");
            mth.append("  if (stack[stackpos].type == 'L') jfvm_arc_release(jvm, &stack[stackpos]);\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xb3:  //putstatic
            //value ->
            idx = readShort() & 0xffff;
            fieldRef = (ConstFieldRef)cls.ConstList[idx];
            scls = cls.getConstString(fieldRef.cls_idx);
            sfield = cls.getConstName(fieldRef.name_type_idx);
            type = cls.getConstType(fieldRef.name_type_idx);
            xcls = clspool.getClass(scls);
            xfield = xcls.getField(sfield + "$" + type);
            mth.append("  jfvm_putstatic(jvm," + addClass(xcls.name) + ",'" + type.charAt(0) + "'," + xfield.clsidx + ", &stack[stackpos]);\n");
            mth.append("  stackpos--;\n");
            break;
          case 0xa9:  //return sub (jsr)
            if (wide) {
              idx = readShort() & 0xffff;
            } else {
              idx = readByte() & 0xff;
            }
            if (idx >= argsCount) {
              slot = "local";
              idx -= argsCount;
            } else {
              slot = "args";
            }
            mth.append("  idx = " + slot + "[" + idx + "].i32;\n");
            mth.append("  goto return_jsr;\n");
            break;
          case 0xb1:  //return void
            mth.append("  goto method_done;\n");
            break;
          case 0x35:  //saload
            // arrayref, index -> value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[2].i32 = temp[1].obj->array->ai16[temp[0].i32];\n");
            pushShort(2);
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x56:  //sastore
            popByte(2);  //value
            popInt(0);  //index
            popObject(1);  //arrayref
            mth.append("  if (temp[1].obj == NULL) jfvm_throw_npe(jvm);\n");
            mth.append("  if (temp[1].obj->array->length < temp[0].i32) jfvm_throw_outofbounds(jvm, temp[1].obj, temp[0].i32);\n");
            mth.append("  temp[1].obj->array->ai16[temp[0].i32] = temp[2].i16;\n");
            mth.append("  jfvm_arc_release(jvm, &temp[1]);\n");
            break;
          case 0x11:  //sipush
            val = readShort();
            pushiShort(val);
            break;
          case 0x5f:  //swap
            swapAny(0,-1);
            break;
          case 0xaa:  //tableswitch
            //read padding
            pad = pc & 0x3;
            if (pad > 0) {
              pad = 0x4 - pad;
              for(int a=0;a<pad;a++) {
                readByte();
              }
            }
            defaultOffset = readInt();
            int low = readInt();
            int high = readInt();
            int length = high - low + 1;
            popInt(0);  //index
            mth.append("  idx = temp[0].i32;\n");
            mth.append("  if ((idx < " + low + ")||(idx > " + high + ")) goto " + "pc_" + (opc + defaultOffset) + ";\n");
            mth.append("  idx -= " + low + ";\n");
            mth.append("  switch (idx) {\n");
            for(int a=0;a<length;a++) {
              int offset = readInt();
              mth.append("    case " + a + ": goto pc_" + (opc + offset) + ";\n");
            }
            mth.append("  }\n");
            break;
          default:
            System.out.println("unknown code:" + Integer.toString(bytecode, 16));
            break;
        }
      }
      postMethod(method);
      mth.append("}\n");  //end of function
      mths.append(mth_pre.toString());
      mths.append(mth.toString());
    } catch (Exception e) {
      e.printStackTrace(System.out);
    }
  }

  public static final int T_BOOLEAN = 4;
  public static final int T_CHAR = 5;
  public static final int T_FLOAT = 6;
  public static final int T_DOUBLE = 7;
  public static final int T_BYTE = 8;
  public static final int T_SHORT = 9;
  public static final int T_INT = 10;
  public static final int T_LONG = 11;

  private String convertType(int type) {
    switch (type) {
      case T_BOOLEAN: return "Z";
      case T_CHAR: return "C";
      case T_FLOAT: return "F";
      case T_DOUBLE: return "D";
      case T_BYTE: return "B";
      case T_SHORT: return "S";
      case T_INT: return "I";
      case T_LONG: return "J";
    }
    return "?";
  }

  private int readByte() throws Exception {
    pc++;
    return dis.readByte();
  }

  private int readShort() throws Exception {
    pc += 2;
    return dis.readShort();
  }

  private int readInt() throws Exception {
    pc += 4;
    return dis.readInt();
  }
/*
  private long readLong() throws Exception {
    pc += 8;
    return dis.readLong();
  }

  private float readFloat() throws Exception {
    pc += 4;
    return dis.readFloat();
  }
*/
  private void addExceptions(AttrCode code) {
    for(int a=0;a<code.exception_count;a++) {
//      CodeException ce = code.exceptions[a];
      mth_pre.append("  UCatch catch_" + a + " = {.type = UCatch_type, .free = 0};\n");
    }
  }

  private void checkException(AttrCode code) {
    for(int a=0;a<code.exception_count;a++) {
      CodeException ce = code.exceptions[a];
      if (pc == ce.start_pc) {
        //add UCatch block to unwinding stack
        String var = "catch_" + a;
        mth.append("  //Try Catch :start=" + ce.start_pc + ",end=" + ce.end_pc + ",handler=" + ce.handler_pc + "\n");
        mth.append("  " + var + ".type = UCatch_type;\n");
        mth.append("  " + var + ".cls = " + addClass(ce.catch_cls) + ";\n");
        mth.append("  " + var + ".prev = jvm->ustack;\n");
        mth.append("#ifdef JFVM_DEBUG_CATCH\n");
        mth.append("  printf(\"   catch:%p:%p:%p\\n\", jvm->thread, &" + var + ", jvm->ustack);\n");
        mth.append("#endif\n");
        mth.append("  jvm->ustack = (UStack*)&" + var + ";\n");
        mth.append("  if (setjmp(" + var + ".buf) != 0) {\n");
        mth.append("    jfvm_stack_release(jvm, &stack, " + stackCount + ");\n");
        mth.append("    stackpos = 0;\n");
        mth.append("    stack[stackpos].obj = jvm->exception;\n");
        mth.append("    stack[stackpos].type = 'L';\n");
        mth.append("    jvm->exception = NULL;\n");
        mth.append("    goto pc_" + ce.handler_pc + ";\n");
        mth.append("  }\n");
      }
      if (pc == ce.end_pc) {
        //remove UCatch block
        mth.append("#ifdef JFVM_DEBUG_CATCH\n");
        mth.append("  printf(\"-  catch:%p:%p:%p\\n\", jvm->thread, jvm->ustack, jvm->ustack->prev);\n");
        mth.append("#endif\n");
        mth.append("  jfvm_ucatch_unwind(jvm);\n");
      }
    }
  }

  private void preMethod(Method method) {
    //define local/spare fields
    mth.append("  Slot temp[3] = {{0,0},{0,0},{0,0}};\n");
    String returnType = method.getReturnCType();
    if (returnType != null) {
      mth.append("  char rtype;\n");  //good game
      mth.append("  " + returnType + " rval;\n");
    }
    //define local vars
    if (localCount > 0) {
      mth.append("  Slot local[" + localCount + "] = {");
      for(int a=0;a<localCount;a++) {
        if (a > 0) mth.append(",");
        mth.append("{0,0}");
      }
      mth.append("};\n");
    }
    //define stack
    mth.append("  Slot stack[" + stackCount + "] = {");
    for(int a=0;a<stackCount;a++) {
      if (a > 0) mth.append(",");
      mth.append("{0,0}");
    }
    mth.append("};\n");
    mth.append("  int stackpos = -1;\n");
    //temp vars
    mth.append("  jint idx, len, val;\n");
    mth.append("  char type;\n");
    mth.append("  Object *obj, *src, *dst;\n");
    //setup unwinding stack
    mth.append("  UMethod *umethod;\n");
    mth.append("  umethod = jfvm_umethod_alloc(jvm, " + quoteString(cls.name) + "," + quoteString(method.name) + ");\n");
    mth.append("  if (setjmp(umethod->buf) != 0) goto method_done;\n");
  }
  private void postMethod(Method method) {
    if (jsridx > 0) {
      mth.append("  goto method_done;\n");
      mth.append("return_jsr:\n");  //idx = return index
      mth.append("switch (idx) {\n");
      for(int a=0;a<jsridx;a++) {
        mth.append("  case " + a + ": goto return_" + a + ";\n");
      }
      mth.append("}\n");
    }
    mth.append("method_done:\n");
    mth.append("#ifdef JFVM_DEBUG_METHOD\n");
    mth.append("  printf(\"m  clean:%p:%s\\n\", jvm->thread, " + quoteString(method.cname) + ");\n");
    mth.append("#endif\n");
    //unwind stack

    if (!isVoid) {
      //move return value on stack to local [0]
      mth.append("  rval = stack[stackpos]." + method.getReturnCName() + ";\n");
      mth.append("  rtype = stack[stackpos].type;\n");
      mth.append("  stack[stackpos].type = 0;\n");
    }
    mth.append("  jfvm_stack_release(jvm, &args[0], " + argsCount + ");\n");
    if (localCount > 0) {
      mth.append("  jfvm_stack_release(jvm, &local[0], " + localCount + ");\n");
    }
    mth.append("  jfvm_stack_release(jvm, &stack[0], " + stackCount + ");\n");
    //in case exception was thrown mid-statement
    mth.append("  jfvm_stack_release(jvm, &temp[0], 3);\n");
    if (!isVoid) {
      //move return value on stack to local [0]
      mth.append("  args[0]." + method.getReturnCName() + " = rval;\n");
      mth.append("  args[0].type = rtype;\n");
    }
    if (method.isSync) {
      mth.append("  jfvm_usync_remove(jvm);\n");
      mth.append("  jfvm_mutex_unlock(method_mutex_" + syncidx + ");\n");
      syncidx++;
    }
    mth.append("#ifdef JFVM_DEBUG_METHOD\n");
    mth.append("  printf(\"m   exit:%p:%s\\n\", jvm->thread, " + quoteString(method.cname) + ");\n");
    mth.append("#endif\n");
    mth.append("  jfvm_umethod_unwind(jvm);\n");
  }

  //push/pop,peek/poke,swap,copy work on the operand stack

  //push immediate values
  private void pushiShort(int val) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].i32 = " + val + ";\n");
    mth.append("  stack[stackpos].type = 'S';\n");
  }
  private void pushiInt(int val) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].i32 = " + val + ";\n");
    mth.append("  stack[stackpos].type = 'I';\n");
  }
  private void pushiLong(long val) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].i64 = " + val + ";\n");
    mth.append("  stack[stackpos].type = 'J';\n");
  }
  private void pushiFloat(float val) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].f32 = " + val + ";\n");
    mth.append("  stack[stackpos].type = 'F';\n");
  }
  private void pushiDouble(double val) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].f64 = " + val + ";\n");
    mth.append("  stack[stackpos].type = 'D';\n");
  }

  private void pushNULL() {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].obj = (void*)NULL;\n");
    mth.append("  stack[stackpos].type = 'L';\n");
  }

  private void pushBoolean(int idx) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].i32 = temp[" + idx + "].i8;\n");
    mth.append("  stack[stackpos].type = 'Z';\n");
  }
  private void pushByte(int idx) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].i32 = temp[" + idx + "].i8;\n");
    mth.append("  stack[stackpos].type = 'B';\n");
  }
  private void pushChar(int idx) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].i32 = temp[" + idx + "].i16;\n");
    mth.append("  stack[stackpos].type = 'C';\n");
  }
  private void pushShort(int idx) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].i32 = temp[" + idx + "].i16;\n");
    mth.append("  stack[stackpos].type = 'S';\n");
  }
  private void pushInt(int idx) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].i32 = temp[" + idx + "].i32;\n");
    mth.append("  stack[stackpos].type = 'I';\n");
  }
  private void pushLong(int idx) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].i64 = temp[" + idx + "].i64;\n");
    mth.append("  stack[stackpos].type = 'J';\n");
  }
  private void pushObject(int idx) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].obj = temp[" + idx + "].obj;\n");
    mth.append("  stack[stackpos].type = 'L';\n");
    mth.append("  temp[" + idx + "].type = 0;\n");
  }
  private void pushAny(int idx) {
    mth.append("  stackpos++;\n");
    mth.append("  stack[stackpos].i64 = temp[" + idx + "].i64;\n");
    mth.append("  stack[stackpos].type = temp[" + idx + "].type;\n");
    mth.append("  temp[" + idx + "].type = 0;\n");
  }

  private void popByte(int idx) {
    mth.append("  temp[" + idx + "].i32=stack[stackpos].i8;\n");
//    out.append("  stack[stackpos].type=0;\n");
    mth.append("  stackpos--;\n");
  }
  private void popChar(int idx) {
    mth.append("  temp[" + idx + "].i32=stack[stackpos].i16;\n");
//    out.append("  stack[stackpos].type=0;\n");
    mth.append("  stackpos--;\n");
  }
  private void popShort(int idx) {
    mth.append("  temp[" + idx + "].i32=stack[stackpos].i16;\n");
//    out.append("  stack[stackpos].type=0;\n");
    mth.append("  stackpos--;\n");
  }
  private void popInt(int idx) {
    mth.append("  temp[" + idx + "].i32=stack[stackpos].i32;\n");
//    out.append("  stack[stackpos].type=0;\n");
    mth.append("  stackpos--;\n");
  }
  private void popLong(int idx) {
    mth.append("  temp[" + idx + "].i64=stack[stackpos].i64;\n");
//    out.append("  stack[stackpos].type=0;\n");
    mth.append("  stackpos--;\n");
  }
  private void popFloat(int idx) {
    mth.append("  temp[" + idx + "].f32=stack[stackpos].f32;\n");
//    out.append("  stack[stackpos].type=0;\n");
    mth.append("  stackpos--;\n");
  }
  private void popDouble(int idx) {
    mth.append("  temp[" + idx + "].f64=stack[stackpos].f64;\n");
//    out.append("  stack[stackpos].type=0;\n");
    mth.append("  stackpos--;\n");
  }
  private void popObject(int idx) {
    mth.append("  temp[" + idx + "].obj=stack[stackpos].obj;\n");
    mth.append("  stack[stackpos].type=0;\n");
    mth.append("  stackpos--;\n");
  }
  private void popAny(int idx) {
    mth.append("  temp[" + idx + "].type=stack[stackpos].type;\n");
    mth.append("  temp[" + idx + "].i64=stack[stackpos].i64;\n");
    mth.append("  stack[stackpos].type=0;\n");
    mth.append("  stackpos--;\n");
  }

  private void swapAny(int i1, int i2) {
    mth.append("  temp[0].i64 = stack[stackpos+" + i1 + "].i64;\n");
    mth.append("  temp[0].type = stack[stackpos+" + i1 + "].type;\n");
    mth.append("  temp[1].i64 = stack[stackpos+" + i2 + "].i64;\n");
    mth.append("  temp[1].type = stack[stackpos+" + i2 + "].type;\n");
    mth.append("  stack[stackpos+" + i1 + "].i64 = temp[1].i64;\n");
    mth.append("  stack[stackpos+" + i1 + "].type = temp[1].type;\n");
    mth.append("  stack[stackpos+" + i2 + "].i64 = temp[0].i64;\n");
    mth.append("  stack[stackpos+" + i2 + "].type = temp[0].type;\n");
  }

  public void insert(int idx) {
    mth.append("  stackpos++;\n");
    mth.append("  for(int a=0;a<" + (-idx) + ";a++) {\n");
    mth.append("    stack[stackpos-a].i64 = stack[stackpos-a-1].i64;\n");
    mth.append("    stack[stackpos-a].type = stack[stackpos-a-1].type;\n");
    mth.append("  }\n");
  }

  public void insert2(int idx) {
    mth.append("  stackpos+=2;\n");
    mth.append("  for(int a=0;a<" + (-idx) + ";a++) {\n");
    mth.append("    stack[stackpos-a].i64 = stack[stackpos-a-2].i64;\n");
    mth.append("    stack[stackpos-a].type = stack[stackpos-a-2].type;\n");
    mth.append("  }\n");
  }

  /** Removes the 'this' pointer from the args stack. */
  public void remove(int cnt) {
    mth.append("  jfvm_arc_release(jvm, &args[0]);\n");
    mth.append("  for(int a=1;a<=" + cnt + ";a++) {\n");
    mth.append("    args[a-1].i64 = args[a].i64;\n");
    mth.append("    args[a-1].type = args[a].type;\n");
    mth.append("  }\n");
    mth.append("  args[" + cnt + "].type = 0;");
  }

  //put work at locals <-> temps

  private void putInt(int lidx, int tidx) {
    String slot;
    if (lidx >= argsCount) {
      slot = "local";
      lidx -= argsCount;
    } else {
      slot = "args";
    }
    mth.append("  " + slot + "[" + lidx + "].i32 = temp[" + tidx + "].i32;\n");
  }
  private void putLong(int lidx, int tidx) {
    String slot;
    if (lidx >= argsCount) {
      slot = "local";
      lidx -= argsCount;
    } else {
      slot = "args";
    }
    mth.append("  " + slot + "[" + lidx + "].i64 = temp[" + tidx + "].i64;\n");
  }
  private void putObject(int lidx, int tidx) {
    String slot;
    if (lidx >= argsCount) {
      slot = "local";
      lidx -= argsCount;
    } else {
      slot = "args";
    }
    mth.append("  " + slot + "[" + lidx + "].obj = temp[" + tidx + "].obj;\n");
    mth.append("  temp[" + tidx + "].type = 0;\n");
  }
  private void putAny(int lidx, int tidx) {
    String slot;
    if (lidx >= argsCount) {
      slot = "local";
      lidx -= argsCount;
    } else {
      slot = "args";
    }
    mth.append("  " + slot + "[" + lidx + "].i64 = temp[" + tidx + "].i64;\n");
    mth.append("  " + slot + "[" + lidx + "].type = temp[" + tidx + "].type;\n");
  }

  private String quoteString(String in) {
    return "\"" + in.replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r") + "\"";
  }

  /* class references are resolved in <clsinit>() which is called from <clinit>() */

  private String addClass(String in) {
    int idx = classes.indexOf(in);
    if (idx == -1) {
      classes.add(in);
      idx = classes.size() - 1;
    }
    return "classref_" + idx;
  }

  private void addClasses() {
    int ccnt = classes.size();
    if (ccnt == 0) return;
    //resolve all class references
    for(int a=0;a<ccnt;a++) {
      cls_pre.append("static Class *classref_" + a + " = NULL;\n");
    }
    for(int a=0;a<syncidx;a++) {
      cls_pre.append("static USync method_usync_" + a + ";\n");
      cls_pre.append("static void* method_mutex_" + a + ";\n");
    }
    cls_pre.append("static void object_clsinit(JVM *jvm, Slot *stack) {\n");
    cls_pre.append("  classref_0 = &" + cls.cname + ";\n");
    for(int a=1;a<ccnt;a++) {
      String scls = classes.get(a);
      cls_pre.append("  classref_" + a + " = jfvm_find_class(jvm, " + quoteString(scls) + ");\n");
    }
    for(int a=0;a<syncidx;a++) {
      cls_pre.append("  method_mutex_" + a + " = jfvm_mutex_alloc();\n");
    }
    cls_pre.append("}\n");
  }

  private void object_init_vtable() {
    //fill in all method pointers in vtable
    cls_pre.append("static void object_init_vtable(JVM *jvm, Slot *args) {\n");
    //invoke object_init_vtable in super class
//    if (jfvmc.debug) cls_pre.append("  printf(\"  vtable:%s\\n\"," + quoteString(cls.name) + ");\n");
    cls_pre.append("  void (*init)(JVM *jvm, Slot *args);\n");
    cls_pre.append("  Class *super = " + cls.cname + ".super_class;\n");
    cls_pre.append("  if (super != NULL) {\n");
    cls_pre.append("    init = super->init_vtable;\n");
    cls_pre.append("    (*init)(jvm, args);\n");
    cls_pre.append("  }\n");
    int cnt = cls.MethodList.length;
    if (cnt == 0) {
      cls_pre.append("}\n");
      return;
    }
    //this = args[0]
    cls_pre.append("  void* vthis = (void*)args[0].obj;\n");
    cls_pre.append("  union { void** ithis;  char *cthis; } u;\n");
    for(int a=0;a<cnt;a++) {
      Method m = cls.MethodList[a];
      if (m.isStatic) continue;
      if (m.isAbstract) continue;
      if (m.name.equals("<clinit>")) continue;
      int offset = m.objidx;
      if (offset == -1) {
        //should not happen
        System.out.println("Error:Method.offset == -1 : " + cls.name + "." + m.name);
        continue;
      }
      cls_pre.append("  u.ithis = (void**)vthis;  u.cthis += sizeof(Object) + (" + offset + " * sizeof(void*));\n");
      cls_pre.append("  *(u.ithis) = &" + m.cname + ";\n");
    }
    cls_pre.append("}\n");
  }

  private String getStackOffsetToArgs(int cnt) {
    if (cnt < 0) {
      return "+" + (-cnt);
    } else if (cnt > 0) {
      return "-" + cnt;
    }
    return "";
  }

  private String getStackCountToArgs(int cnt) {
    if (cnt < 0) {
      return "+=" + (-cnt);
    } else if (cnt > 0) {
      return "-=" + cnt;
    }
    return "";
  }

  private static String mnemonics[] = {
    "nop",
    "aconst_null",
    "iconst_m1",
    "iconst_0",
    "iconst_1",
    "iconst_2",
    "iconst_3",
    "iconst_4",
    "iconst_5",
    "lconst_0",
    "lconst_1",
    "fconst_0",
    "fconst_1",
    "fconst_2",
    "dconst_0",
    "dconst_1",
    "bipush",
    "sipush",
    "ldc",
    "ldc_w",
    "ldc2_w",
    "iload",
    "lload",
    "fload",
    "dload",
    "aload",
    "iload_0",
    "iload_1",
    "iload_2",
    "iload_3",
    "lload_0",
    "lload_1",
    "lload_2",
    "lload_3",
    "fload_0",
    "fload_1",
    "fload_2",
    "fload_3",
    "dload_0",
    "dload_1",
    "dload_2",
    "dload_3",
    "aload_0",
    "aload_1",
    "aload_2",
    "aload_3",
    "iaload",
    "laload",
    "faload",
    "daload",
    "aaload",
    "baload",
    "caload",
    "saload",
    "istore",
    "lstore",
    "fstore",
    "dstore",
    "astore",
    "istore_0",
    "istore_1",
    "istore_2",
    "istore_3",
    "lstore_0",
    "lstore_1",
    "lstore_2",
    "lstore_3",
    "fstore_0",
    "fstore_1",
    "fstore_2",
    "fstore_3",
    "dstore_0",
    "dstore_1",
    "dstore_2",
    "dstore_3",
    "astore_0",
    "astore_1",
    "astore_2",
    "astore_3",
    "iastore",
    "lastore",
    "fastore",
    "dastore",
    "aastore",
    "bastore",
    "castore",
    "sastore",
    "pop",
    "pop2",
    "dup",
    "dup_x1",
    "dup_x2",
    "dup2",
    "dup2_x1",
    "dup2_x2",
    "swap",
    "iadd",
    "ladd",
    "fadd",
    "dadd",
    "isub",
    "lsub",
    "fsub",
    "dsub",
    "imul",
    "lmul",
    "fmul",
    "dmul",
    "idiv",
    "ldiv",
    "fdiv",
    "ddiv",
    "irem",
    "lrem",
    "frem",
    "drem",
    "ineg",
    "lneg",
    "fneg",
    "dneg",
    "ishl",
    "lshl",
    "ishr",
    "lshr",
    "iushr",
    "lushr",
    "iand",
    "land",
    "ior",
    "lor",
    "ixor",
    "lxor",
    "iinc",
    "i2l",
    "i2f",
    "i2d",
    "l2i",
    "l2f",
    "l2d",
    "f2i",
    "f2l",
    "f2d",
    "d2i",
    "d2l",
    "d2f",
    "i2b",
    "i2c",
    "i2s",
    "lcmp",
    "fcmpl",
    "fcmpg",
    "dcmpl",
    "dcmpg",
    "ifeq",
    "ifne",
    "iflt",
    "ifge",
    "ifgt",
    "ifle",
    "if_icmpeq",
    "if_icmpne",
    "if_icmplt",
    "if_icmpge",
    "if_icmpgt",
    "if_icmple",
    "if_acmpeq",
    "if_acmpne",
    "goto",
    "jsr",
    "ret",
    "tableswitch",
    "lookupswitch",
    "ireturn",
    "lreturn",
    "freturn",
    "dreturn",
    "areturn",
    "return",
    "getstatic",
    "putstatic",
    "getfield",
    "putfield",
    "invokevirtual",
    "invokespecial",
    "invokestatic",
    "invokeinterface",
    "invokedynamic",
    "new",
    "newarray",
    "anewarray",
    "arraylength",
    "athrow",
    "checkcast",
    "instanceof",
    "monitorenter",
    "monitorexit",
    "wide",
    "multianewarray",
    "ifnull",
    "ifnonnull",
    "goto_w",
    "jsr_w",
    "breakpoint"
  };
}
