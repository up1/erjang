/**
 * This file is part of Erjang - A JVM-based Erlang VM
 *
 * Copyright (c) 2009 by Trifork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package erjang.beam.analysis;

import org.objectweb.asm.Type;

import erjang.EObject;
import erjang.ETuple;

class TypeMap {
	private static Type TYPE_EOBJECT = Type.getType(EObject.class);
	private static Type TYPE_ETUPLE = Type.getType(ETuple.class);
	private static String ETUPLE_NAME = TYPE_ETUPLE.getInternalName();

	private static Type[] NO_TYPES = new Type[0];
	private Type[] xregs, yregs, fregs;
	final int stacksize; // number of y-regs

	final BasicBlock bb;

	public TypeMap(BasicBlock bb) {
		xregs = NO_TYPES;
		yregs = NO_TYPES;
		fregs = NO_TYPES;
		stacksize = 0;
		this.bb = bb;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("[");

		boolean first = true;

		Type[] xregs = this.xregs;

		first = printRegs(sb, first, xregs, 'x');
		first = printRegs(sb, first, fregs, 'f');
		first = printYRegs(sb, first);

		sb.append(", s=" + stacksize);

		sb.append(']');
		return sb.toString();

	}

	private boolean printRegs(StringBuilder sb, boolean first, Type[] regs,
			char reg) {
		for (int i = 0; i < regs.length; i++) {
			if (regs[i] != null) {
				if (first == false) {
					sb.append(", ");
				} else {
					first = false;
				}
				sb.append(reg).append(i).append(':');
				sb.append(shortName(regs[i]));
			}
		}
		return first;
	}

	private boolean printYRegs(StringBuilder sb, boolean first) {
		for (int i = 0; i < stacksize; i++) {
			if (i >= yregs.length)
				continue;
			if (yregs[i] != null) {
				if (first == false) {
					sb.append(", ");
				} else {
					first = false;
				}
				sb.append('y').append(get_ypos(i)).append(':');
				sb.append(shortName(yregs[i]));
			}
		}
		return first;
	}

	private String shortName(Type type) {
		if (type.getSort() != Type.OBJECT) {
			return type.getDescriptor();
		}

		String in = type.getInternalName();
		if (type.getSort() == Type.OBJECT) {
			int idx = in.lastIndexOf('/');
			String sh = in.substring(idx + 1);

			if (sh.length() > 6 && sh.startsWith("ETuple")) {
				return "T" + sh.substring(6);
			} else {
				return sh.substring(1, 3);
			}
		} else {
			return in;
		}
	}

	private TypeMap(Type[] xregs, Type[] yregs, Type[] fregs, int stacksize,
			BasicBlock bb) {
		super();
		this.xregs = xregs;
		this.yregs = yregs;
		this.fregs = fregs;
		this.stacksize = stacksize;
		this.bb = bb;
	}

	public boolean equals(Object obj) {
		if (obj instanceof TypeMap) {
			TypeMap other = (TypeMap) obj;

			return eq(xregs, other.xregs) && eqy(this, other)
					&& eq(fregs, other.fregs);
		}

		return false;
	}

	private static boolean eqy(TypeMap me, TypeMap other) {
		if (me.stacksize != other.stacksize)
			return false;

		int stacksize = Math.min(me.stacksize, other.stacksize);
		for (int i = 0; i < stacksize; i++) {
			if (!eq(me.gety(i), other.gety(i)))
				return false;
		}
		return true;
	}

	private boolean eq(Type[] r1, Type[] r2) {
		int max = Math.max(r1.length, r2.length);
		for (int i = 0; i < max; i++) {
			if (!eq(get(r1, i), get(r2, i)))
				return false;
		}
		return true;
	}

	private static boolean eq(Type t1, Type t2) {
		if (t1 == t2)
			return true;
		if (t1 == null || t2 == null)
			return false;
		return t1.equals(t2);
	}

	public TypeMap mergeFrom(TypeMap other) {
		Type[] new_x = eq(xregs, other.xregs) ? xregs : merge_regs(xregs,
				other.xregs);
		Type[] new_f = eq(fregs, other.fregs) ? fregs : merge_regs(fregs,
				other.fregs);

		Type[] new_y = yregs;
		int new_stacksize = stacksize;
		if (!eqy(this, other)) {

			if (stacksize != other.stacksize) {
				// yank stack!
				new_stacksize = 0;
				new_y = NO_TYPES;
			} else {

				// the smaller stack-size wins
				new_stacksize = Math.min(stacksize, other.stacksize);
				new_y = new Type[new_stacksize];
				for (int i = 0; i < new_stacksize; i++) {
					new_y[new_stacksize - i - 1] = merge(this.gety(i), other
							.gety(i));
				}
			}
		}

		if (new_x == xregs && new_y == yregs && new_f == fregs) {
			return this;
		} else {
			return new TypeMap(new_x, new_y, new_f, new_stacksize, bb);
		}
	}

	private Type[] merge_regs(Type[] r1, Type[] r2) {
		Type[] res = new Type[Math.max(r1.length, r2.length)];
		for (int i = 0; i < res.length; i++) {
			Type t1 = get(r1, i);
			Type t2 = get(r2, i);

			res[i] = merge(t1, t2);
		}
		return res;
	}

	private Type merge(Type t1, Type t2) {
		if (t1 == null || t2 == null)
			return null;
		if (t1.equals(t2))
			return t1;

		if (t1.getSort() == Type.OBJECT && t2.getSort() == Type.OBJECT) {
			if (t1.getInternalName().startsWith(ETUPLE_NAME)
					&& t2.getInternalName().startsWith(ETUPLE_NAME)) {
				return TYPE_ETUPLE;
			}
		}

		return TYPE_EOBJECT;
	}

	private Type get(Type[] regs, int i) {
		if (i < regs.length)
			return regs[i];
		else
			return null;
	}

	public TypeMap setx(int reg, Type t) {
		bb.kill_x(reg);

		if (eq(getx(reg), t))
			return this;

		Type[] new_xregs;
		if (xregs.length <= reg) {
			new_xregs = grow(xregs, reg);
		} else {
			new_xregs = copy(xregs);
		}
		new_xregs[reg] = t;
		return new TypeMap(new_xregs, yregs, fregs, stacksize, bb);
	}

	public TypeMap setf(int reg, Type t) {
		bb.kill_fr(reg);

		if (eq(getf(reg), t))
			return this;
		Type[] new_fregs;
		if (fregs.length <= reg) {
			new_fregs = grow(fregs, reg);
		} else {
			new_fregs = copy(fregs);
		}
		new_fregs[reg] = t;
		return new TypeMap(xregs, yregs, new_fregs, stacksize, bb);
	}

	private Type[] copy(Type[] regs) {
		Type[] res = new Type[regs.length];
		for (int i = 0; i < regs.length; i++) {
			res[i] = regs[i];
		}
		return res;
	}

	private static Type[] grow(Type[] regs, int reg) {
		Type[] res = new Type[reg + 6];
		for (int i = 0; i < regs.length; i++) {
			res[i] = regs[i];
		}
		return res;
	}

	public Type getx(int reg) {
		bb.use_x(reg);

		if (reg >= xregs.length) {
			return null;
		} else {
			return xregs[reg];
		}
	}

	public Type getf(int reg) {
		bb.use_fr(reg);

		if (reg >= fregs.length) {
			return null;
		} else {
			return fregs[reg];
		}
	}

	public TypeMap sety(int reg, Type t) {
		bb.kill_y(this, reg);

		if (eq(gety(reg), t))
			return this;

		int pos = get_ypos(reg);

		if (pos < 0 || pos >= stacksize)
			throw new IllegalArgumentException("No Y" + reg + " register here.");

		Type[] new_yregs;
		if (yregs.length <= pos) {
			new_yregs = grow(yregs, pos);
		} else {
			new_yregs = copy(yregs);
		}
		new_yregs[pos] = t;
		return new TypeMap(xregs, new_yregs, fregs, stacksize, bb);
	}

	public Type gety(int reg) {
		bb.use_y(this, reg);

		int pos = get_ypos(reg);
		if (pos < 0 || pos >= stacksize)
			throw new IllegalArgumentException("no Y" + reg + " register");
		if (pos >= yregs.length)
			return null;
		return yregs[pos];
	}

	public int get_ypos(int reg) {
		return stacksize - reg - 1;
	}

	public TypeMap trim_y(int howmuch) {
		return new TypeMap(xregs, yregs, fregs, stacksize - howmuch, bb);
	}

	public TypeMap alloc_y(int howmuch) {
		return new TypeMap(xregs, yregs, fregs, stacksize + howmuch, bb);
	}

	public TypeMap clearLive(BasicBlock bb) {
		return new TypeMap(xregs, yregs, fregs, stacksize, bb);
	}

	public void add_succ(BasicBlock succ) {
		bb.succ(succ);
	}

	public void touchx(int from, int to) {
		for (int i = from; i < to; i++) {
			bb.use_x(i);
		}
	}

	public int max_xreg() {
		int max = 0;
		for (int i = 0; i < xregs.length; i++) {
			if (xregs[i] != null)
				max = i;
		}
		return max + 1;
	}

	public int max_freg() {
		int max = 0;
		for (int i = 0; i < fregs.length; i++) {
			if (fregs[i] != null)
				max = i;
		}
		return max + 1;
	}

}