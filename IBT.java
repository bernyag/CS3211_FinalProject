import java.util.*;
public class IBT extends Thread{
	private List<URLHTMLTuple> BUL;
	public IBT( List<URLHTMLTuple> BUL){
		this.BUL=BUL;
	}
	public void transfer(List<URLHTMLTuple> BUL){
		for (URLHTMLTuple temp : BUL){
			//synchronization needed
			if(!IUT.search(temp)){//IUT is the global URLtree
				IUT.insert(temp);
			}
			//synchronization end
		}
	}

	public void run(){
		while(true){
			if(BUL.size() ==  1000){
			transfer(BUL);
			BUL.clear();
		}
	}
	}
}


