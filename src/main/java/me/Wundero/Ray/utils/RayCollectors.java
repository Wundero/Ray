package me.Wundero.Ray.utils;

import java.util.List;
import java.util.stream.Collector;

/*
 The MIT License (MIT)

 Copyright (c) 2016 Wundero

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

/**
 * Some collectors for streams that are useful.
 */
public class RayCollectors {

	/**
	 * Create a synch list from a stream
	 */
	public static <T> Collector<T, List<T>, List<T>> syncList() {
		return Collector.of(() -> Utils.sl(), List::add, (a, b) -> {
			a.addAll(b);
			return a;
		});
	}
	
	/**
	 * Create an array list from a stream
	 */
	public static <T> Collector<T, List<T>, List<T>> rayList() {
		return Collector.of(() -> Utils.al(), List::add, (a, b) -> {
			a.addAll(b);
			return a;
		});
	}

}
