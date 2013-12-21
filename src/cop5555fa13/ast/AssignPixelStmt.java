package cop5555fa13.ast;

import cop5555fa13.TokenStream.Token;

public class AssignPixelStmt extends AssignStmt {

	final Token lhsIdent;
	final Pixel pixel;
	boolean ispixel;
	
	public AssignPixelStmt(Token lhsIdent, Pixel pixel) {
		super();
		this.lhsIdent = lhsIdent;
		this.pixel = pixel;
	}
	
	public void setflag(boolean val) {
		ispixel = val;
	}
	
	@Override
	public Object visit(ASTVisitor v, Object arg) throws Exception {
		return v.visitAssignPixelStmt(this, arg);
	}

}
