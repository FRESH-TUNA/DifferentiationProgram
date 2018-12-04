package 과제5;

import java.io.IOException;
import java.io.StreamTokenizer;

enum NodeType {CONSTANT, VARIABLE, OPERATOR}

abstract class ExpTreeNode {
	public abstract NodeType getType();
}


class ConstantNode extends ExpTreeNode{
	public double number;
	public ConstantNode(double number) {
		this.number = number;
	}
	public NodeType getType() {
		return NodeType.CONSTANT;
	}
	public double getNumber() {
		return number;
	}
}

class IdentifierNode extends ExpTreeNode{
	public String name;
	public IdentifierNode(String name) {
		this.name = name;
	}
	public NodeType getType() {
		return NodeType.VARIABLE;
	}
	public String getVar() {
		return name;
	}
}

class OperatorNode extends ExpTreeNode {
	public char op;
	public ExpTreeNode left;
	public ExpTreeNode right;
	public OperatorNode(char op, ExpTreeNode left, ExpTreeNode right) {
		this.op = op;
		this.left = left;
		this.right = right;
	}
	public NodeType getType() {
		return NodeType.OPERATOR;
	}
	public char getOp() {
		return op;
	}
	public ExpTreeNode getLeft() {
		return left;
	}
	public ExpTreeNode getRight() {
		return right;
	}
}


public class ExpTree {
	public ExpTreeNode root;
	public ExpTreeNode differentedRoot;
	public ExpTreeNode simplifiedRoot;
	private StreamTokenizer in; 

	public ExpTree(StreamTokenizer in) {
		this.in = in;	
		root = null;
		in.ordinaryChar('/');	// '/'는 주석문을 구성하는 문자로 여겨지므로
		//  이것을 벗어나 나누기 문자로 사용하기 위함
		in.ordinaryChar('-');	// a-b는 하나의 변수로 볼 수 있으므로 이것을 
		// 벗어나 빼기 문자로 사용하기 위함
	}

	public void buildExpTree() throws IOException {
		root = readStatement();
		differentedRoot = differentation();
		simplifiedRoot = simplified();
	}

	// Statement ::= Identifier '=' Expression ';'
	public ExpTreeNode readStatement() {
		return readExpression();
	}

	// Expression ::= Term { ( '+' | '-' ) Term }	
	public ExpTreeNode readExpression() {
		ExpTreeNode termTree = readTerm();
		while(true) {
			try {
				in.nextToken();
			}
			catch(IOException e) {
				System.out.println(e.getMessage());
			}
			char op = (char)in.ttype;

			switch(in.ttype) {
			case '+' : termTree = new OperatorNode(op, termTree, readTerm()); break;
			case '-' : termTree = new OperatorNode(op, termTree, readTerm()); break;
			default : in.pushBack(); return termTree;
			}
		}
	}

	// Term ::= Factor { ( '*' | '/' ) Factor }	
	public ExpTreeNode readTerm() {
		ExpTreeNode factorTree = readFactor();
		while(true) {
			try {
				in.nextToken();
			}
			catch(IOException e) {
				System.out.println(e.getMessage());
			}
			switch(in.ttype) {
			case '*' : factorTree = new OperatorNode((char)in.ttype, factorTree, readFactor()); break;
			case '/' : factorTree = new OperatorNode((char)in.ttype, factorTree, readFactor()); break;
			case '^' : factorTree = new OperatorNode((char)in.ttype, factorTree, readFactor()); break;
			default : in.pushBack(); return factorTree;
			}
		}
	}

	// Factor ::= Identifier | Number | '(' Expression ')' | '-' Factor
	public ExpTreeNode readFactor() {
		try {
			in.nextToken();
			if(in.ttype == '-')
				return new OperatorNode((char)in.ttype, new ConstantNode(-1), readFactor());
			else if(in.ttype == -3) {
				return new IdentifierNode(in.sval);
			}
			else if(in.ttype == -2) {
				return new ConstantNode(in.nval);
			}
			else if(in.ttype == '(') {
				ExpTreeNode tempNode = null;
				while(in.ttype != ')') {
					tempNode = readExpression();
				}
				try {
					in.nextToken();
					return tempNode;
				}
				catch(IOException e) {
					System.out.println(e.getMessage());
				}
			}
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	public ExpTreeNode differentation() {
		return differentation(root);
	}
	private ExpTreeNode differentation(ExpTreeNode tree) {
		if(tree.getType().equals(NodeType.OPERATOR)) {
			OperatorNode temp = (OperatorNode) tree;
			OperatorNode newTree;

			if(temp.getOp() == '+' || temp.getOp() == '-') {
				newTree = new OperatorNode(temp.getOp(),
						differentation(temp.left),
						differentation(temp.right));
			}
			else if(temp.getOp() == '*') {
				newTree = new OperatorNode('+', 
						new OperatorNode('*', temp.left, differentation(temp.right)),
						new OperatorNode('*', differentation(temp.left), temp.right));
			}
			else if(temp.getOp() == '^') {
				ConstantNode constant = (ConstantNode) temp.right;
				IdentifierNode node = (IdentifierNode) temp.left;

				if(node.getVar().equals("x")) {
					newTree = new OperatorNode('*', 
							new ConstantNode(constant.getNumber()),
							new OperatorNode('^', new IdentifierNode("x"), new ConstantNode(constant.getNumber() - 1)));
				}
				else 
					return new ConstantNode(0);

			}
			else {
				newTree = new OperatorNode('/',
						new OperatorNode('-',
								new OperatorNode('*', differentation(temp.left), temp.right),
								new OperatorNode('*', temp.left, differentation(temp.right))),
						new OperatorNode('*', temp.right, temp.right));
			}
			return newTree;
		}
		else if(tree.getType().equals(NodeType.CONSTANT)) {
			return new ConstantNode(0);
		}
		else {
			IdentifierNode temp = (IdentifierNode) tree;
			if(!temp.getVar().equals("x"))
				return new ConstantNode(0);
			else
				return new ConstantNode(1);
		}
	}

	public ExpTreeNode simplified() {
		return simplified(differentedRoot);
	}
	private ExpTreeNode simplified(ExpTreeNode tree) {
		if(tree.getType().equals(NodeType.OPERATOR)) {
			OperatorNode temp = (OperatorNode) tree;
			
			//연산자가 '+'인경우
			if(temp.getOp() == '+') {
				if(temp.right.getType().equals(NodeType.CONSTANT) && !temp.left.getType().equals(NodeType.CONSTANT)) {
					ConstantNode constant = (ConstantNode)temp.right;
					if(constant.number == 0)
						return simplified(temp.left);
				}
				if(temp.left.getType().equals(NodeType.CONSTANT) && !temp.right.getType().equals(NodeType.CONSTANT)) {
					ConstantNode constant = (ConstantNode)temp.left;
					if(constant.number == 0)
						return simplified(temp.right);
				}
				if(temp.left.getType().equals(NodeType.CONSTANT) && temp.right.getType().equals(NodeType.CONSTANT)) {
					ConstantNode constant = (ConstantNode)temp.left;
					ConstantNode constant1 = (ConstantNode)temp.right;
					return new ConstantNode(constant.getNumber() + constant1.getNumber());
				}
				
					OperatorNode newTree = new OperatorNode(temp.getOp(),
							simplified(temp.left),
							simplified(temp.right));
					if(newTree.left.getType().equals(NodeType.CONSTANT) && newTree.right.getType().equals(NodeType.CONSTANT)){
						ConstantNode constant = (ConstantNode)newTree.left;
						ConstantNode constant1 = (ConstantNode)newTree.right;
						return new ConstantNode(constant.getNumber() + constant1.getNumber());
					}
					else if(newTree.left.getType().equals(NodeType.CONSTANT) && newTree.right.getType().equals(NodeType.OPERATOR)){
						ConstantNode constant = (ConstantNode)newTree.left;
						OperatorNode constant1 = (OperatorNode)newTree.right;
						newTree = new OperatorNode(temp.getOp(), constant, simplified(constant1));
						if(newTree.right.getType().equals(NodeType.CONSTANT))
							return simplified(newTree);
						else
							return newTree;
					}
					else if(newTree.left.getType().equals(NodeType.OPERATOR) && newTree.right.getType().equals(NodeType.CONSTANT)) {
						OperatorNode constant = (OperatorNode)newTree.left;
						ConstantNode constant1 = (ConstantNode)newTree.right;
						newTree = new OperatorNode(temp.op, simplified(constant), constant1);
						if(newTree.left.getType().equals(NodeType.CONSTANT))
							return simplified(newTree);
						else
							return newTree;
					}
					else {
						return newTree;
					}
			}
			
			//연산자가 '-'인경우
			else if(temp.getOp() == '-') {
				if(temp.right.getType().equals(NodeType.CONSTANT)) {
					ConstantNode constant = (ConstantNode)temp.right;
					if(constant.number == 0)
						return simplified(temp.left);
				}
				if(temp.left.getType().equals(NodeType.VARIABLE) && temp.right.getType().equals(NodeType.VARIABLE)) 
					return new ConstantNode(0);

				OperatorNode newTree = new OperatorNode(temp.getOp(),
						simplified(temp.left),
						simplified(temp.right));
				if(newTree.left.getType().equals(NodeType.CONSTANT) && newTree.right.getType().equals(NodeType.CONSTANT)){
					ConstantNode constant = (ConstantNode)newTree.left;
					ConstantNode constant1 = (ConstantNode)newTree.right;
					return new ConstantNode(constant.getNumber() - constant1.getNumber());
				}
				else if(newTree.left.getType().equals(NodeType.CONSTANT) && newTree.right.getType().equals(NodeType.OPERATOR)){
					ConstantNode constant = (ConstantNode)newTree.left;
					OperatorNode constant1 = (OperatorNode)newTree.right;
					newTree = new OperatorNode(temp.getOp(), constant, simplified(constant1));
					if(newTree.right.getType().equals(NodeType.CONSTANT))
						return simplified(newTree);
					else
						return newTree;
				}
				else if(newTree.left.getType().equals(NodeType.OPERATOR) && newTree.right.getType().equals(NodeType.CONSTANT)) {
					OperatorNode constant = (OperatorNode)newTree.left;
					ConstantNode constant1 = (ConstantNode)newTree.right;
					newTree = new OperatorNode(temp.op, simplified(constant), constant1);
					if(newTree.left.getType().equals(NodeType.CONSTANT))
						return simplified(newTree);
					else
						return newTree;
				}
				else {
					return newTree;
				}
			}
			//연산자가 '*'인경우
			else if(temp.getOp() == '*') {
				if(temp.right.getType().equals(NodeType.CONSTANT) && temp.left.getType().equals(NodeType.CONSTANT)) {
					ConstantNode leftConstant = (ConstantNode)temp.left;
					ConstantNode rightConstant = (ConstantNode)temp.right;
					return new ConstantNode(leftConstant.getNumber() * rightConstant.getNumber());
				}
				if(temp.right.getType().equals(NodeType.CONSTANT)) {
					ConstantNode constant = (ConstantNode)temp.right;
					if(constant.number == 0)
						return new ConstantNode(0);
					else if(constant.number == 1) 
						return simplified(temp.left);
				}
				if(temp.left.getType().equals(NodeType.CONSTANT)) {
					ConstantNode constant = (ConstantNode)temp.left;
					if(constant.number == 0)
						return new ConstantNode(0);
					else if(constant.number == 1) 
						return simplified(temp.right);
				}

				OperatorNode newTree = new OperatorNode(temp.getOp(),
						simplified(temp.left),
						simplified(temp.right));
				if(newTree.left.getType().equals(NodeType.CONSTANT) && newTree.right.getType().equals(NodeType.CONSTANT)) {
					ConstantNode leftConstant = (ConstantNode)newTree.left;
					ConstantNode rightConstant = (ConstantNode)newTree.right;
					return new ConstantNode(leftConstant.getNumber() * rightConstant.getNumber());
				}
				else if(newTree.left.getType().equals(NodeType.CONSTANT) && newTree.right.getType().equals(NodeType.OPERATOR)){
					ConstantNode constant = (ConstantNode)newTree.left;
					OperatorNode constant1 = (OperatorNode)newTree.right;
					newTree = new OperatorNode(temp.getOp(), constant, simplified(constant1));
					if(newTree.right.getType().equals(NodeType.CONSTANT))
						return simplified(newTree);
					else
						return newTree;
				}
				else if(newTree.left.getType().equals(NodeType.OPERATOR) && newTree.right.getType().equals(NodeType.CONSTANT)) {
					OperatorNode constant = (OperatorNode)newTree.left;
					ConstantNode constant1 = (ConstantNode)newTree.right;
					newTree = new OperatorNode(temp.op, simplified(constant), constant1);
					if(newTree.left.getType().equals(NodeType.CONSTANT))
						return simplified(newTree);
					else
						return newTree;
				}
				else 
					return newTree;
			}
			//연산자가 '^'인 경우
			else if(temp.getOp() == '^') {
				if(temp.right.getType().equals(NodeType.CONSTANT)) {
					ConstantNode constant = (ConstantNode)temp.right;
					if(constant.number == 0)
						return new ConstantNode(0);
					else if(constant.number == 1)
						return simplified(temp.left);
				}
				OperatorNode newTree = new OperatorNode(temp.getOp(),
						temp.left,
						temp.right);
				return newTree;
			}
			else {
				if(temp.left.getType().equals(NodeType.VARIABLE)) {
					if(temp.right.getType().equals(NodeType.VARIABLE))
						return new ConstantNode(1);
					if(temp.right.getType().equals(NodeType.CONSTANT)) {
						ConstantNode constant = (ConstantNode)temp.right;
						if(constant.number == 1)
							return simplified(temp.left);
					}	
				}
				
				OperatorNode newTree = new OperatorNode(temp.getOp(),
						simplified(temp.left),
						simplified(temp.right));
				
				if(newTree.left.getType().equals(NodeType.CONSTANT) && newTree.right.getType().equals(NodeType.CONSTANT)) {
					ConstantNode leftConstant = (ConstantNode)newTree.left;
					ConstantNode rightConstant = (ConstantNode)newTree.right;
					return new ConstantNode(leftConstant.getNumber() / rightConstant.getNumber());
				}
				else if(newTree.left.getType().equals(NodeType.CONSTANT) && newTree.right.getType().equals(NodeType.OPERATOR)){
					ConstantNode constant = (ConstantNode)newTree.left;
					OperatorNode constant1 = (OperatorNode)newTree.right;
					newTree = new OperatorNode(temp.getOp(), constant, simplified(constant1));
					if(newTree.right.getType().equals(NodeType.CONSTANT)) {
						ConstantNode leftConstant = (ConstantNode)newTree.left;
						ConstantNode rightConstant = (ConstantNode)newTree.right;
						return new ConstantNode(leftConstant.getNumber() / rightConstant.getNumber());
					}
					else
						return newTree;
				}
				else if(newTree.left.getType().equals(NodeType.OPERATOR) && newTree.right.getType().equals(NodeType.CONSTANT)) {
					OperatorNode constant = (OperatorNode)newTree.left;
					ConstantNode constant1 = (ConstantNode)newTree.right;
					newTree = new OperatorNode(temp.op, simplified(constant), constant1);
					if(newTree.left.getType().equals(NodeType.CONSTANT)) {
						ConstantNode leftConstant = (ConstantNode)newTree.left;
						ConstantNode rightConstant = (ConstantNode)newTree.right;
						return new ConstantNode(leftConstant.getNumber() / rightConstant.getNumber());
					}
					else
						return newTree;
				}
				else 
					return newTree;
			}
		}
		else 
			return tree;
	}

	public void print(ExpTreeNode tree) {
		if(tree != null){
			if(tree.getType().equals(NodeType.OPERATOR)) {
				OperatorNode temp = (OperatorNode) tree;
				System.out.print("( ");
				print(temp.getLeft());
				System.out.print(" "+temp.getOp()+" ");
				print(temp.getRight());
				System.out.print(" )");
			}
			else if(tree.getType().equals(NodeType.CONSTANT)) {
				ConstantNode temp = (ConstantNode) tree;
				System.out.print(temp.getNumber());
			}
			else{
				IdentifierNode temp = (IdentifierNode) tree;
				System.out.print(temp.getVar());
			}
		}
	}

	public void inorderTraverse(ExpTreeNode tree) { // 표현 트리를 왼쪽으로 90도 회전하여 시각적으로 출력
		inorderTraverse(tree, 0);
	}
	private void inorderTraverse(ExpTreeNode tree, int count) {
		if(tree != null){
			if(tree.getType().equals(NodeType.OPERATOR)) {
				OperatorNode temp = (OperatorNode) tree;

				inorderTraverse(temp.getRight(), count + 1);
				for(int i = 0; i < count; ++i)
					System.out.print('\t');
				System.out.println(temp.getOp());
				inorderTraverse(temp.getLeft(), count + 1);
			}
			else if(tree.getType().equals(NodeType.CONSTANT)) {
				ConstantNode temp = (ConstantNode) tree;
				for(int i = 0; i < count; ++i)
					System.out.print('\t');
				System.out.println(temp.getNumber());
			}
			else{
				IdentifierNode temp = (IdentifierNode) tree;
				for(int i = 0; i < count; ++i)
					System.out.print('\t');
				System.out.println(temp.getVar());
			}
		}
	}
}
