package me.Wundero.ProjectRay.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

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
public abstract class DataHolder {

	protected HashMap<String, Object> data = Maps.newHashMap();

	protected UUID uuid;

	public synchronized void putAll(Map<String, Object> values) {
		data.putAll(values);
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> T getData(String key) {
		return (T) data.get(key);
	}

	public synchronized boolean hasData(String key) {
		return data.containsKey(key);
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> T removeData(String key) {
		return (T) data.remove(key);
	}

	public synchronized Object putData(String key, Object value) {
		return data.put(key, value);
	}

	public synchronized void clearData() {
		data.clear();
	}

	public synchronized UUID getUUID() {
		return uuid;
	}

	public synchronized void setUUID(UUID u) {
		this.uuid = u;
	}

}
