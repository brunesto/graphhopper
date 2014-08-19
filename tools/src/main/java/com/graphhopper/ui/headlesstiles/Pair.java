package com.graphhopper.ui.headlesstiles;

import java.io.Serializable;

/**
 * 
 * A  generic pair of values .
 * 
 * hack: setters must be declared so that gilead/beanlib can replicate the beans
 * 
 */
public  class Pair<A, B> implements Serializable,Comparable<Pair<A, B>>  {

  static final long serialVersionUID=1;
  A a;
  B b;

  @Override
  public int hashCode()
  {
    int retVal=0;
    retVal+=a==null?0:a.hashCode();
    retVal+=b==null?0:b.hashCode();
    return retVal;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if (o instanceof Pair) {
      Pair<A, B> oAsPair=(Pair<A, B>)o; 
      if (a==null && oAsPair.a!=null) return false;
      if (a!=null && !a.equals(oAsPair.a)) return false;
      if (b==null && oAsPair.b!=null) return false;
      if (b!=null && !b.equals(oAsPair.b)) return false;
      return true;
      
    }
    return false;
  }
  
  // -- generated code below --
  
  /**
   * empty constructor, for GWT serialization only
   */
  public Pair() {
  }
  
  public Pair(A a, B b) {
    this.a = a;
    this.b = b;
  }

  public A getA() {
    return a;
  }

  public B getB() {
    return b;
  }
  
  
 

  public void setA(A a) {
	this.a = a;
}

public void setB(B b) {
	this.b = b;
}

/**
   * Constructs a <code>String</code> with all attributes
   * in name = value format.
   *
   * @return a <code>String</code> representation 
   * of this object.
   */
  @Override
  public String toString()
  {
      String retValue = "";
      retValue = " "
          + "(" + this.a
          + "," + this.b+")"; 
      return retValue;
  }

@SuppressWarnings("unchecked")
@Override
public int compareTo(Pair<A, B> o) {
	
	int aCompare=((Comparable<A>)a).compareTo(o.a);
	if (aCompare!=0)
		return aCompare;
	int bCompare=((Comparable<B>)b).compareTo(o.b);
	return bCompare;
	
}



}
