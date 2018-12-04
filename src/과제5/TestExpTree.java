package ����5;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;

public class TestExpTree {
	public static void main(String[] args) throws IOException, CloneNotSupportedException {		
		StreamTokenizer in  = 	new StreamTokenizer(
					new BufferedReader (
					new InputStreamReader((new FileInputStream("input.txt")))));
		ExpTree tree = new ExpTree(in);

		while (true) {
			in.nextToken();
			//������ ���̸� ����������.
			if (in.ttype == StreamTokenizer.TT_EOF)
				break;
			in.pushBack();
			try {
				tree.buildExpTree();	// ǥ�� Ʈ�� ����
			}
			catch (IOException e) {
				System.out.println("\n" + e.getMessage());
				System.exit(1);
			}
			
			System.out.print("Output tree: ");
			tree.print(tree.root);
			System.out.println();
			tree.inorderTraverse(tree.root);
			System.out.print("\nDifferented tree: ");
			tree.print(tree.differentedRoot);
			System.out.println();
			tree.inorderTraverse(tree.differentedRoot);
		
			System.out.print("\nSimplifed: ");
			tree.print(tree.simplifiedRoot);
			System.out.println("\n**********************************************");
			
			in.nextToken();
		}
	}
}
