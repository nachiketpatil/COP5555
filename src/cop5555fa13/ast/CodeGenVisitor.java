package cop5555fa13.ast;

import static cop5555fa13.TokenStream.Kind.*;
import static cop5555fa13.TokenStream.Kind;

import java.util.HashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cop5555fa13.CompilerErrorException;
import cop5555fa13.runtime.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	private ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
	private String progName;

	private int slot = 0;

	private int getSlot(String name) {
		Integer s = slotMap.get(name);
		if (s != null)
			return s;
		else {
			slotMap.put(name, slot);
			return slot++;
		}
	}

	HashMap<String, Integer> slotMap = new HashMap<String, Integer>();

	// map to look up JVM types correspondingHashMap<K, V> language
	static final HashMap<Kind, String> typeMap = new HashMap<Kind, String>();
	static {
		typeMap.put(_int, "I");
		typeMap.put(pixel, "I");
		typeMap.put(_boolean, "Z");
		typeMap.put(image, "Lcop5555fa13/runtime/PLPImage;");
	}

	@Override
	public Object visitDec(Dec dec, Object arg) throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;
		// insert source line number info into classfile
		Label l = new Label();
		mv.visitLabel(l);
		mv.visitLineNumber(dec.ident.getLineNumber(), l);
		// get name and type
		String varName = dec.ident.getText();
		Kind t = dec.type;
		String jvmType = typeMap.get(t);
		Object initialValue = (t == _int || t == pixel || t == _boolean) ? Integer
				.valueOf(0) : null;
		// add static field to class file for this variable
		FieldVisitor fv = cw.visitField(ACC_STATIC, varName, jvmType, null,
				initialValue);
		fv.visitEnd();
		// if this is an image, generate code to create an empty image
		if (t == image) {
			mv.visitTypeInsn(NEW, PLPImage.className);
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, PLPImage.className, "<init>",
					"()V");
			mv.visitFieldInsn(PUTSTATIC, progName, varName, typeMap.get(image));
		}
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;
		String sourceFileName = (String) arg;
		progName = program.getProgName();
		String superClassName = "java/lang/Object";

		// visit the ClassWriter to set version, attributes, class name and
		// superclass name
		cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, progName, null, superClassName,
				null);
		// Optionally, indicate the name of the source file
		cw.visitSource(sourceFileName, null);
		// initialize creation of main method
		String mainDesc = "([Ljava/lang/String;)V";
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", mainDesc, null,
				null);
		mv.visitCode();
		Label start = new Label();
		mv.visitLabel(start);
		mv.visitLineNumber(program.ident.getLineNumber(), start);

		// visit children
		for (Dec dec : program.decList) {
			dec.visit(this, mv);
		}

		for (Stmt stmt : program.stmtList) {
			stmt.visit(this, mv);
		}

		// add a return statement to the main method
		mv.visitInsn(RETURN);

		// finish up
		Label end = new Label();
		mv.visitLabel(end);
		// visit local variables. The one is slot 0 is the formal parameter of
		// the main method.
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, start, end,
				getSlot("args"));
		mv.visitLocalVariable("x", typeMap.get(Kind._int), null, start, end,
				getSlot("x"));
		mv.visitLocalVariable("y", typeMap.get(Kind._int), null, start, end,
				getSlot("y"));
		// if there are any more local variables, visit them now.
		// ......

		// finish up method
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		// convert to bytearray and return
		return cw.toByteArray();
	}

	@Override
	public Object visitAlternativeStmt(AlternativeStmt alternativeStmt,
			Object arg) throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		// create the labels
		Label elseLabel = new Label();
		Label endLabel = new Label();

		// visit the expression
		alternativeStmt.expr.visit(this, mv);

		// check the condition and jump to appropriate label.
		mv.visitJumpInsn(Opcodes.IFEQ, elseLabel);

		// visit the statements in if part
		for (Stmt stmt : alternativeStmt.ifStmtList) {
			stmt.visit(this, mv);
		}
		// jump to end skipping else part.
		mv.visitJumpInsn(Opcodes.GOTO, endLabel);

		// visit the else label
		mv.visitLabel(elseLabel);

		// visit the statements in else part
		for (Stmt stmt : alternativeStmt.elseStmtList) {
			stmt.visit(this, mv);
		}
		mv.visitLabel(endLabel);

		return null;
	}

	@Override
	public Object visitPauseStmt(PauseStmt pauseStmt, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		// visit the expressions.
		pauseStmt.expr.visit(this, mv);

		// generate code to update the pixel.
		mv.visitMethodInsn(INVOKESTATIC, PLPImage.className, "pause",
				PLPImage.pauseDesc);

		return null;
	}

	@Override
	public Object visitIterationStmt(IterationStmt iterationStmt, Object arg)
			throws Exception {
		//System.out.println("IterationStmt");

		MethodVisitor mv = (MethodVisitor) arg;

		Label guardLabel = new Label();
		Label bodyLabel = new Label();

		mv.visitJumpInsn(Opcodes.GOTO, guardLabel);
		mv.visitLabel(bodyLabel);

		// visit the statements in while loop
		for (Stmt stmt : iterationStmt.stmtList) {
			stmt.visit(this, mv);
		}

		mv.visitLabel(guardLabel);

		// visit the expression
		iterationStmt.expr.visit(this, mv);

		// check the condition
		mv.visitJumpInsn(Opcodes.IFNE, bodyLabel);

		return null;
	}

	@Override
	public Object visitAssignPixelStmt(AssignPixelStmt assignPixelStmt,
			Object arg) throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		// if lhs is pixel
		if (assignPixelStmt.ispixel) {

			// visit the pixel to generate code to leave value on top of stack
			assignPixelStmt.pixel.visit(this, mv);

			// store the value in the pixel named by lhs.
			mv.visitFieldInsn(PUTSTATIC, progName,
					assignPixelStmt.lhsIdent.getText(), typeMap.get(Kind.pixel));
		} else {
			// lhs is an image

			Label outerGuard = new Label();
			Label outerBody = new Label();
			Label innerGuard = new Label();
			Label innerBody = new Label();
			
			// load the image.
			mv.visitFieldInsn(GETSTATIC, progName, assignPixelStmt.lhsIdent.getText(), PLPImage.classDesc);
			mv.visitInsn(DUP);
			mv.visitInsn(DUP);
			
			// get image parameters.
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getWidth", "()I");
			mv.visitVarInsn(ISTORE,getSlot("localWidth"));			
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getHeight", "()I");	
			mv.visitVarInsn(ISTORE,getSlot("localHeight"));
			
			// Initialize outer loop variable 'x' to 0
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE,getSlot("x"));

			mv.visitJumpInsn(Opcodes.GOTO, outerGuard);
			mv.visitLabel(outerBody);

			// initialize inner loop variable 'y' to 0
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE,getSlot("y"));

			/***************************************************/
			// visit y statement
			mv.visitJumpInsn(Opcodes.GOTO, innerGuard);
			mv.visitLabel(innerBody);

			// Body part start///////////////////////////////////////////
			// This is SinglePixelAssignmentStmt in a loop over x and y

			// first get the image.
			String name = assignPixelStmt.lhsIdent.getText();
			mv.visitFieldInsn(GETSTATIC, progName, name, PLPImage.classDesc);

			// Load x and y expressions.
			mv.visitVarInsn(ILOAD, getSlot("x"));
			mv.visitVarInsn(ILOAD, getSlot("y"));

			// visit the pixel
			assignPixelStmt.pixel.visit(this, mv);

			// generate code to update the pixel.
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "setPixel",
					"(III)V");

			// increase y by 1
			mv.visitIincInsn(getSlot("y"), 1);

			mv.visitLabel(innerGuard);
			
			// condition check
			mv.visitVarInsn(ILOAD, getSlot("y"));
			mv.visitVarInsn(ILOAD,getSlot("localHeight"));
			
			// check the condition
			mv.visitJumpInsn(Opcodes.IF_ICMPLT, innerBody);
			/*************************************************/

			mv.visitIincInsn(getSlot("x"), 1);

			mv.visitLabel(outerGuard);
			
			mv.visitVarInsn(ILOAD, getSlot("x"));
			mv.visitVarInsn(ILOAD,getSlot("localWidth"));	
			// check the condition
			mv.visitJumpInsn(Opcodes.IF_ICMPLT, outerBody);

			// Update the frame
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className,
					"updateFrame", PLPImage.updateFrameDesc);
		}
		return null;
	}

	@Override
	public Object visitPixel(Pixel pixel, Object arg) throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		pixel.redExpr.visit(this, mv);
		pixel.greenExpr.visit(this, mv);
		pixel.blueExpr.visit(this, mv);

		mv.visitMethodInsn(INVOKESTATIC,
				cop5555fa13.runtime.Pixel.JVMClassName, "makePixel",
				cop5555fa13.runtime.Pixel.makePixelSig);

		return null;
	}

	@Override
	public Object visitSinglePixelAssignmentStmt(
			SinglePixelAssignmentStmt singlePixelAssignmentStmt, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		// first get the image.
		String imageName = singlePixelAssignmentStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName, imageName, PLPImage.classDesc);

		// duplicate the address.
		mv.visitInsn(DUP);

		// visit the expressions.
		singlePixelAssignmentStmt.xExpr.visit(this, mv);
		singlePixelAssignmentStmt.yExpr.visit(this, mv);
		singlePixelAssignmentStmt.pixel.visit(this, mv);

		// generate code to update the pixel.
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "setPixel",
				"(III)V");

		// generate code to update frame, consuming the second image address.
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "updateFrame",
				PLPImage.updateFrameDesc);

		return null;
	}

	@Override
	public Object visitSingleSampleAssignmentStmt(
			SingleSampleAssignmentStmt singleSampleAssignmentStmt, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		// first get the image.
		String imageName = singleSampleAssignmentStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName, imageName, PLPImage.classDesc);

		// duplicate the address.
		mv.visitInsn(DUP);

		// visit the expressions.
		singleSampleAssignmentStmt.xExpr.visit(this, mv);
		singleSampleAssignmentStmt.yExpr.visit(this, mv);
		
		String color = singleSampleAssignmentStmt.color.getText();
		int colorCode = -1;
		if (color.equalsIgnoreCase("red")) {
			colorCode = ImageConstants.RED;
		} else if (color.equalsIgnoreCase("green")) {
			colorCode = ImageConstants.GRN;
		} else if (color.equalsIgnoreCase("blue")) {
			colorCode = ImageConstants.BLU;
		} else {
			throw new CompilerErrorException("Invalid color name. must be one of {'red' , 'green', 'blue'}");
		}
		mv.visitLdcInsn(colorCode);
		singleSampleAssignmentStmt.rhsExpr.visit(this, mv);

		// generate code to update the pixel.
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "setSample",
				"(IIII)V");

		// generate code to update frame, consuming the second image address.
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "updateFrame",
				PLPImage.updateFrameDesc);

		return null;
	}

	@Override
	public Object visitScreenLocationAssignmentStmt(
			ScreenLocationAssignmentStmt screenLocationAssignmentStmt,
			Object arg) throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		// store the values in appropriate fields of the image indicated by
		// ident.
		// first get the image.
		String imageName = screenLocationAssignmentStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName, imageName, PLPImage.classDesc);

		// visit the x_Loc expression.
		mv.visitInsn(DUP);
		screenLocationAssignmentStmt.xScreenExpr.visit(this, mv);
		// set the x_loc
		mv.visitFieldInsn(PUTFIELD, PLPImage.className, "x_loc", typeMap.get(Kind._int));

		// visit the y_Loc expression.
		mv.visitInsn(DUP);
		screenLocationAssignmentStmt.yScreenExpr.visit(this, mv);
		// set the y_loc
		mv.visitFieldInsn(PUTFIELD, PLPImage.className, "y_loc", typeMap.get(Kind._int));

		// generate code to update frame, consuming the second image address.
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "updateFrame",
				PLPImage.updateFrameDesc);
		return null;
	}

	@Override
	public Object visitShapeAssignmentStmt(
			ShapeAssignmentStmt shapeAssignmentStmt, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		// first get the image.
		String imageName = shapeAssignmentStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName, imageName, PLPImage.classDesc);

		// visit the height expression.
		mv.visitInsn(DUP);
		shapeAssignmentStmt.height.visit(this, mv);
		// set the height
		mv.visitFieldInsn(PUTFIELD, PLPImage.className, "height", typeMap.get(Kind._int));

		// visit the width expression.
		mv.visitInsn(DUP);
		shapeAssignmentStmt.width.visit(this, mv);
		// set the width
		mv.visitFieldInsn(PUTFIELD, PLPImage.className, "width", typeMap.get(Kind._int));

		// update the height and width of the image
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className,
				"updateImageSize", PLPImage.updateFrameDesc);

		// update the frame of image
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "updateFrame",
				PLPImage.updateFrameDesc);
		return null;
	}

	@Override
	public Object visitSetVisibleAssignmentStmt(
			SetVisibleAssignmentStmt setVisibleAssignmentStmt, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;
		// generate code to leave image on top of stack
		String imageName = setVisibleAssignmentStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName, imageName, PLPImage.classDesc);
		// duplicate address. Will consume one for updating setVisible field
		// and one for invoking updateFrame.
		mv.visitInsn(DUP);
		// visit expr on rhs to leave its value on top of the stack
		setVisibleAssignmentStmt.expr.visit(this, mv);
		// set visible field
		mv.visitFieldInsn(PUTFIELD, PLPImage.className, "isVisible", "Z");
		// generate code to update frame, consuming the second image address.
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "updateFrame",
				PLPImage.updateFrameDesc);
		return null;
	}

	@Override
	public Object FileAssignStmt(cop5555fa13.ast.FileAssignStmt fileAssignStmt,
			Object arg) throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;
		// generate code to leave address of target image on top of stack
		String image_name = fileAssignStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName, image_name, typeMap.get(image));
		// generate code to duplicate this address. We'll need it for loading
		// the image and again for updating the frame.
		mv.visitInsn(DUP);
		// generate code to leave address of String containing a filename or url
		mv.visitLdcInsn(fileAssignStmt.fileName.getText().replace("\"", ""));
		// generate code to get the image by calling the loadImage method
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "loadImage",
				PLPImage.loadImageDesc);
		// generate code to update frame
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "updateFrame",
				PLPImage.updateFrameDesc);
		return null;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr,
			Object arg) throws Exception {
		
		MethodVisitor mv = (MethodVisitor) arg;

		// create the labels
		Label falseLabel = new Label();
		Label endLabel = new Label();

		// visit the expression.
		conditionalExpr.condition.visit(this, mv);

		// check the condition and jump to appropriate label.
		mv.visitJumpInsn(Opcodes.IFEQ, falseLabel);

		// visit the statements in if part
		conditionalExpr.trueValue.visit(this, mv);
		// jump to end skipping else part.
		mv.visitJumpInsn(Opcodes.GOTO, endLabel);

		// visit the else label
		mv.visitLabel(falseLabel);

		// visit the statements in else part
		conditionalExpr.falseValue.visit(this, mv);

		mv.visitLabel(endLabel);

		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		// visit e0 and e1 so that they leave their values on stack.
		binaryExpr.e0.visit(this, mv);
		binaryExpr.e1.visit(this, mv);

		// based on the operator, evaluate the value of the expression.
		if (binaryExpr.op.kind == Kind.PLUS) {
			mv.visitInsn(IADD);
		} else if (binaryExpr.op.kind == Kind.MINUS) {
			mv.visitInsn(ISUB);
		} else if (binaryExpr.op.kind == Kind.TIMES) {
			mv.visitInsn(IMUL);
		} else if (binaryExpr.op.kind == Kind.DIV) {
			mv.visitInsn(IDIV);
		} else if (binaryExpr.op.kind == Kind.MOD) {
			mv.visitInsn(IREM);
		} else if (binaryExpr.op.kind == Kind.LSHIFT) {
			mv.visitInsn(ISHL);
		} else if (binaryExpr.op.kind == Kind.RSHIFT) {
			mv.visitInsn(ISHR);
		} else if (binaryExpr.op.kind == Kind.AND) {
			mv.visitInsn(IAND);
		} else if (binaryExpr.op.kind == Kind.OR) {
			mv.visitInsn(IOR);
		} else if (binaryExpr.op.kind == Kind.EQ) {

			Label trueLabel = new Label();
			Label endLabel = new Label();
			// check the condition EQ
			mv.visitJumpInsn(Opcodes.IF_ICMPEQ, trueLabel);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(Opcodes.GOTO, endLabel);
			mv.visitLabel(trueLabel);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(endLabel);

		} else if (binaryExpr.op.kind == Kind.NEQ) {

			Label trueLabel = new Label();
			Label endLabel = new Label();
			// check the condition NEQ
			mv.visitJumpInsn(Opcodes.IF_ICMPNE, trueLabel);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(Opcodes.GOTO, endLabel);
			mv.visitLabel(trueLabel);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(endLabel);

		} else if (binaryExpr.op.kind == Kind.GT) {

			Label trueLabel = new Label();
			Label endLabel = new Label();
			// check the condition GT
			mv.visitJumpInsn(Opcodes.IF_ICMPGT, trueLabel);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(Opcodes.GOTO, endLabel);
			mv.visitLabel(trueLabel);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(endLabel);

		} else if (binaryExpr.op.kind == Kind.GEQ) {

			Label trueLabel = new Label();
			Label endLabel = new Label();
			// check the condition GE
			mv.visitJumpInsn(Opcodes.IF_ICMPGE, trueLabel);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(Opcodes.GOTO, endLabel);
			mv.visitLabel(trueLabel);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(endLabel);

		} else if (binaryExpr.op.kind == Kind.LT) {

			Label trueLabel = new Label();
			Label endLabel = new Label();
			// check the condition LT
			mv.visitJumpInsn(Opcodes.IF_ICMPLT, trueLabel);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(Opcodes.GOTO, endLabel);
			mv.visitLabel(trueLabel);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(endLabel);

		} else if (binaryExpr.op.kind == Kind.LEQ) {

			Label trueLabel = new Label();
			Label endLabel = new Label();
			// check the condition LE
			mv.visitJumpInsn(Opcodes.IF_ICMPLE, trueLabel);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(Opcodes.GOTO, endLabel);
			mv.visitLabel(trueLabel);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(endLabel);

		} else {
			throw new CompilerErrorException("Invalid Operand found");
		}

		return null;
	}

	@Override
	public Object visitSampleExpr(SampleExpr sampleExpr, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		// get the image.
		String image_name = sampleExpr.ident.getText();
		mv.visitFieldInsn(GETSTATIC, progName, image_name, typeMap.get(image));

		// evaluate the expressions.
		sampleExpr.xLoc.visit(this, mv);
		sampleExpr.yLoc.visit(this, mv);

		String color = sampleExpr.color.getText();
		int colorCode = -1;
		if (color.equalsIgnoreCase("red")) {
			colorCode = ImageConstants.RED;
		} else if (color.equalsIgnoreCase("green")) {
			colorCode = ImageConstants.GRN;
		} else if (color.equalsIgnoreCase("blue")) {
			colorCode = ImageConstants.BLU;
		} else {
			throw new CompilerErrorException("Invalid color name. must be one of {'red' , 'green', 'blue'}");
		}
		mv.visitLdcInsn(colorCode);

		// generate code to update the pixel.
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getSample",
				"(III)I");

		return null;
	}

	@Override
	public Object visitImageAttributeExpr(
			ImageAttributeExpr imageAttributeExpr, Object arg) throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		// get the image
		String image_name = imageAttributeExpr.ident.getText();
		mv.visitFieldInsn(GETSTATIC, progName, image_name, typeMap.get(image));

		if(imageAttributeExpr.selector.kind == Kind.x_loc) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getX_loc", "()I");
		} else if(imageAttributeExpr.selector.kind == Kind.y_loc) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getY_loc", "()I");
		} else if(imageAttributeExpr.selector.kind == Kind.height) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getHeight", "()I");
		} else if(imageAttributeExpr.selector.kind == Kind.width) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getWidth", "()I");
		} else {
			throw new CompilerErrorException("Invalid image attribute.");
		}

		return null;
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitFieldInsn(GETSTATIC, progName, identExpr.ident.getText(),
				typeMap.get(identExpr.type));
		return null;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;
		int val = intLitExpr.intLit.getIntVal();
		mv.visitLdcInsn(val);
		return null;
	}

	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;
		String lit = booleanLitExpr.booleanLit.getText();
		int val = lit.equals("true") ? 1 : 0;
		mv.visitLdcInsn(val);
		return null;
	}

	@Override
	public Object visitPreDefExpr(PreDefExpr PreDefExpr, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;

		if (PreDefExpr.type == Kind.Z) {
			mv.visitLdcInsn(ImageConstants.Z);
		} else if (PreDefExpr.type == Kind.SCREEN_SIZE) {
			mv.visitLdcInsn(PLPImage.SCREENSIZE);
		} else if (PreDefExpr.type == Kind.x) {
			mv.visitVarInsn(ILOAD, getSlot("x"));
		} else if (PreDefExpr.type == Kind.y) {
			mv.visitVarInsn(ILOAD, getSlot("y"));
		} else if (PreDefExpr.type == Kind._int) {
			if (PreDefExpr.constantLit.kind == Kind.Z) {
				mv.visitLdcInsn(ImageConstants.Z);
			} else {
				mv.visitLdcInsn(PreDefExpr.constantLit.getIntVal());
			}
		}
		return null;
	}

	@Override
	public Object visitAssignExprStmt(AssignExprStmt assignExprStmt, Object arg)
			throws Exception {

		MethodVisitor mv = (MethodVisitor) arg;
		// Visit the right hand side expression.
		assignExprStmt.expr.visit(this, mv);
		// set the value of right hand side expression to left hand side
		// variable.
		if (assignExprStmt.expr.type == Kind.SCREEN_SIZE) {
			assignExprStmt.expr.type = Kind._int;
		}
		mv.visitFieldInsn(PUTSTATIC, progName,
				assignExprStmt.lhsIdent.getText(),
				typeMap.get(assignExprStmt.expr.type));
		return null;
	}

}
