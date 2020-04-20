using System;
using System.Collections.Generic;
using System.Text;
using PAT.Common.Classes.Expressions.ExpressionClass;

//the namespace must be PAT.Lib, the class and method names can be arbitrary
namespace PAT.Lib
{
	/** Datastucture for storing the Indexed URL Tree */
    public class NiklasTree: ExpressionValue
    {
        public System.Collections.Generic.Queue<int> queue;
		public bool locked;
        
        //default constructor
        public NiklasTree() 
        {
            this.queue = new System.Collections.Generic.Queue<int>();
			this.locked = false;
        }
        
        public NiklasTree(System.Collections.Generic.Queue<int> queue)
        {
            this.queue = queue;
        }
		

        //override
		/** Returns clone of object (Used by PAT during simulation) */
        public override ExpressionValue  GetClone()
        {
        	NiklasTree clone = new NiklasTree(new System.Collections.Generic.Queue<int>(this.queue));
        	clone.locked = this.locked;
 	        return clone;
 	         
        }

		/** Returns string representation of the tree (used for visualization during simulation)*/
        public override string  ToString()
        {
            return "[" + ExpressionID + "], locked: " +this.isLocked().ToString();
        }

		/** Called by ToString*/
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


		/** Locks the tree */
		public void doLock()
		{
			this.locked = true;
		}
		/** Unlocks the tree */
		public void doUnlock()
		{
			this.locked = false;
		}
		public bool isLocked()
		{
			return this.locked;
		}
		/** Returns true if there are any duplicates in the tree */
		public bool hasDuplicates()
		{
			bool tmp = false;
			int[] arr  = this.queue.ToArray();
			for (int i = 0; i < arr.Length ; i++)
			{
				for (int j = 0; j < arr.Length ; j++)
				{
					if (i!=j && arr[i]==arr[j])
					{
						tmp = true;
					}
				}
			}
			
			return tmp;
		}
		
		/** Writes an element to the tree */
		public bool Write(int element)
		{
			Enqueue(element);
			return true;
		}
		
		/** USE WRITE() INSTEAD!! Adds element to underllying queue */
         public void Enqueue(int element)
        {
            this.queue.Enqueue(element);
        }
		/** Removes element from underlying queue */
        public void Dequeue()
        {
            if (this.queue.Count > 0)
            {
                queue.Dequeue();
            }
            else
            {
            
                //throw PAT Runtime exception
                throw new RuntimeException("Access an empty queue!");
            }

        }
		/** Checks for the existance of an element in the queue */
        public bool Contains(int element)
        {
            return this.queue.Contains(element);
        }
		
		/** Returns first element of underlying queue */
        public int First()
        {
            if (this.queue.Count > 0)
            {
                return this.queue.ToArray()[0];
            }
            else
            {
            
                //throw PAT Runtime exception
                throw new RuntimeException("Access an empty queue!");
                
            }

        }

		/** Returns last element of underlying queue */
        public int Last()
        {
            if (queue.Count > 0)
            {
                return this.queue.ToArray()[queue.Count -1];
            }
            else 
            {
                //throw PAT Runtime exception
                throw new RuntimeException("Access an empty queue!");
              
            }
            
        }

		/** Clears the underlying queue */
        public void Clear()
        {
            this.queue.Clear();
        }
	
		/** Returns number of elements in the tree */
        public int Count()
        {
            return queue.Count;
        }
    
    }
}
