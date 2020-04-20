using System;
using System.Collections.Generic;
using System.Text;
using PAT.Common.Classes.Expressions.ExpressionClass;

//the namespace must be PAT.Lib, the class and method names can be arbitrary
namespace PAT.Lib
{
    public class NiklasBuffers: ExpressionValue
    {
    	/** This class stores an array of buffers. The Buffer class is a nested class*/
		private Buffer[] buffers;
		
		// Constructors
		public NiklasBuffers(int numBuffers)
		{
			this.buffers = new Buffer[numBuffers];
			for (int i = 0; i < this.buffers.Length ; i++)
			{
					this.buffers[i] = new Buffer();
			}

		}
		public NiklasBuffers(int numBuffers, int bufferSize)
		{
			this.buffers = new Buffer[numBuffers];
			for (int i = 0; i < this.buffers.Length ; i++)
			{
					this.buffers[i] = new Buffer(bufferSize);
			}

		}

		/** Adds a certain value to one of the buffers */
		public void fill(int bufferNo, int val)
		{
			this.buffers[bufferNo].fill(val);
		}
		/** Returns the first element of a certain buffer and removes that element from the buffer*/
		public int useFirst(int bufferNo)
		{
			return this.buffers[bufferNo].useFirst();
		}
		/** Returns the first element of a certain buffer */
		public int getFirst(int bufferNo)
		{
			return this.buffers[bufferNo].First();
		}
		/** Checks if a certain buffer is full */
		public bool isFull(int bufferNo)
		{
			return this.buffers[bufferNo].isFull();
		}
		/** Checks if ALL buffers are empty */
		public bool isEmptied()
		{
			int tmp = 0;
			for (int i = 0; i < this.buffers.Length ; i++)
			{
					tmp += this.buffers[i].Count();
			}
			return tmp == 0;
		}
		/** Checks if ALL buffers are unlocked */
		public bool areUnlocked()
		{
			for (int i = 0; i < this.buffers.Length ; i++)
			{
				if (isLocked(i))
				{
					return false;
				}
			}
			return true;
		}
		
		/** Checks if a certain buffer is locked */
		public bool isLocked(int bufferNo)
		{
			return this.buffers[bufferNo].isLocked();
		}
		/** Checks if a certain buffer is empty */
		public bool isEmpty(int bufferNo)
		{
			return this.buffers[bufferNo].Count()==0;
		}
		/** Sets whether or not a certain buffer should be locked */
		public void setLocked(int bufferNo, bool locked)
		{
			this.buffers[bufferNo].setLocked(locked);
		}
		
		
		/** Returns a clone of the given object. (This is frequently called by PAT during simulation) */
		public override ExpressionValue  GetClone()
        {
        	NiklasBuffers clone = new NiklasBuffers(this.buffers.Length);
       		//Buffer[] clone = new Buffer[this.buffers.Length];
       		for (int i = 0; i < this.buffers.Length ; i++)
				{
					clone.buffers[i] = this.buffers[i].GetClone();
				}
 	         return clone;
        }
        
        
        //override
        /** Returns string representation. Used in the simulator to display datastructure */
        public override string  ToString()
        {
            return "{ \n" + ExpressionID + "}, isEmptied: " + isEmptied().ToString();
        }

        public override string ExpressionID
        {
        	get
        	{
        		string my_string = "";
        		for (int i = 0; i < this.buffers.Length ; i++)
				{
    				my_string += buffers[i].ToString() + "\n";
				}				
        		return my_string;
        	}
        }
        
        /** Class describing each individual buffer */
		public class Buffer
		{
			private System.Collections.Generic.Queue<int> queue;
			private int size;
			private bool locked;
			
			// constructors
			public Buffer()
			{
				this.queue = new System.Collections.Generic.Queue<int>();
				this.locked = false;
				this.size = 0;
			}			
			public Buffer(int size)
			{
				this.queue = new System.Collections.Generic.Queue<int>();
				this.locked = false;
				this.size = size;
			}
			
			// Setters and getters
			public void setLocked(bool locked)
			{
				this.locked = locked;
			}
			public void setSize(int size)
			{
				this.size = size;
			}
			public int getSize()
			{
				return this.size;
			}
			public bool isLocked()
			{
				return this.locked;
			}
			public bool isFull()
			{
				return this.size == Count();
			}
			/** Returns a clone of the given object. (Called indirecly by parent class) */
			public Buffer GetClone()
        	{
        		Buffer clone = new Buffer();
        		clone.queue = new System.Collections.Generic.Queue<int>(this.queue);
        		clone.size = this.size;
        		clone.locked = this.locked;
 	         	return clone;
        	}
			
			//Override
			/** Returns string representation. Used in the simulator to display datastructure */
			public override string  ToString()
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
            	return "[" +returnString + "], locked: " + isLocked().ToString() + ", size: " + getSize().ToString();
        	}
        	
        	/** Returns number of element in buffer */
        	public int Count()
        	{
            	return queue.Count;
        	}
        	
        	/** Gets first element of buffer */
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
        	
        	/** Checks if an elemnt is present in the buffer */
        	public bool Contains(int element)
        	{
            	return this.queue.Contains(element);
        	}
        	
        	/** Use fill() instead!!!! (Inserts leement into buffer.) */
        	public void Enqueue(int element) 
        	{
            	this.queue.Enqueue(element);
        	}
        	
        	/** Removes first element of buffer */
        	public void Dequeue()
        	{
        		bool tmp = this.locked;
            	if (this.queue.Count > 0)
            	{
                	queue.Dequeue();
            	}
            	else
            	{
                	//throw PAT Runtime exception
                	throw new RuntimeException("Access an empty queue!");
            	}
				this.locked = tmp;
        	}
        	
        	/** Returns and removes the first element of teh buffer */
        	public int useFirst()
			{
				int element = this.First();
				this.Dequeue();
			
				return element;
			}
			
			/** Adds an element to the buffer */
			public void fill(int element)
			{
			
				if (isFull())
				{
					this.locked = true;
					throw new RuntimeException("Cant fill when full!");
			
				}
				else
				{
					this.Enqueue(element);
				}
			}		
		}
	}
}