using System;
using System.Collections.Generic;
using System.Text;
using PAT.Common.Classes.Expressions.ExpressionClass;

//the namespace must be PAT.Lib, the class and method names can be arbitrary
namespace PAT.Lib
{
	/** Queue datastructure used to store the set of "gimmic URLs" */
    public class Queue: ExpressionValue
    {
        public System.Collections.Generic.Queue<int> queue;
        
        //default constructor
        public Queue() 
        {
            this.queue = new System.Collections.Generic.Queue<int>();
        }
        
		
        public Queue(int max, int seed)
        {
            this.queue = new System.Collections.Generic.Queue<int>();
            
			Random randNum = new Random(seed);
			for (int i = 0; i <max ; i++)
			{
    			queue.Enqueue(randNum.Next(0, max));
			}
        }
		
        
        public Queue(System.Collections.Generic.Queue<int> queue)
        {
            this.queue = queue;
        }

        //override
        /** Returnsa clone of the object (called by PAT during simulation) */
        public override ExpressionValue  GetClone()
        {
 	         return new Queue(new System.Collections.Generic.Queue<int>(this.queue));
        }
        
        /** Returns string representation of queue. Used for visualisation in the simulation */
        public override string  ToString()
        {
            return "[" + ExpressionID + "], isEmpty: " +isEmpty().ToString();
        }
		
		/** Called by ToString */
        public override string ExpressionID
        {
            get
            {
                string returnString = "";
                foreach (int element in this.queue)
                {
                    returnString += element.ToString() + ",";
                }
                if (returnString.Length > 0)
                {
                    returnString = returnString.Substring(0, returnString.Length - 1);
                }
                return returnString;
            }
        }

		
		/** Returns and removes the first elements in the queue */
		public int useFirst()
		{
			int element = this.First();
			this.Dequeue();
			
			return element;
		}
		
		public bool isEmpty()
		{
			return this.Count()==0;
		}

  		/** Adds elements to the queue */
		public void Enqueue(int element)
        {
            this.queue.Enqueue(element);
        }

		/** Removes first elements in the queue */
        public void Dequeue()
        {
            if (this.queue.Count > 0)
            {
                queue.Dequeue();
            }
            else
            {

            }
        }
        
		/** Returns first elements in the queue */
        public int First()
        {
            if (this.queue.Count > 0)
            {
                return this.queue.ToArray()[0];
            }
            else
            {            	
            	return -1;
                //throw PAT Runtime exception
                throw new RuntimeException("Access an empty queue in First!");                
            }
        }
        
		/** Returns number of elements in the queue */
        public int Count()
        {
            return queue.Count;
        }
    
    }
}
