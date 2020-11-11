package com.single.patch.plugin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author Lance
 * @date 2019-09-23
 */
public class ClassUtils {


    /**
     *
     * @param inputStream  class文件的输入流
     * @return
     * @throws IOException
     */
    public static byte[] referHackWhenInit(InputStream inputStream) throws IOException {
        ClassReader cr = new ClassReader(inputStream);//class的解析器
        ClassWriter cw = new ClassWriter(cr, 0);//class的输出器
        //class的访问者，相当于回调，解析器解析的结果，回调给访问者
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, final String name, String desc,
                                             String signature, String[] exceptions) {

                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new MethodVisitor(api, mv) {
                    @Override
                    public void visitInsn(int opcode) {
                        //在构造方法中插入AntilazyLoad引用
                        if ("<init>".equals(name) && opcode == Opcodes.RETURN) {
                            super.visitLdcInsn(Type.getType("Lcom/single/patch/hack/AntilazyLoad;"));
                        }
                        super.visitInsn(opcode);
                    }
                };
                return mv;
            }

        };
        //启动分析，启动后ClassReader 开始解析class数据，然后回调访问者的visitMethod
        cr.accept(cv, 0);
        return cw.toByteArray();
    }
}
