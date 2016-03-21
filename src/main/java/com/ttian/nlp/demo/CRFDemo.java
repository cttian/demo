package com.ttian.nlp.demo;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class CRFDemo {

	enum Status {
		S("S", 0), B("B", 1), M("M", 2), E("E", 3);
		private String name;
		private int index;
		Status(String name, int index){
			this.name = name;
			this.index = index;
		}
		
	};

	static class Feature implements Serializable{
		private int count; // 总量
		private double[] status; // observation squence transition matrix
		private double[][] transfer; // the state transition matrix
		private HashMap<String, Double> preStatus;
		private HashMap<String, Double> nextStatus;

		public Feature(String[][] context) {
			this.count = 0;
			this.status = new double[4];
			Arrays.fill(status, 0.);
			this.transfer = new double[4][4];
			Arrays.asList(this.transfer).forEach(e -> Arrays.fill(e, 0.));
			this.preStatus = new HashMap<String, Double>();
			this.nextStatus = new HashMap<String, Double>();
			this.count(context);
		}

		public void count(String[][] context) {
			assert context[1].length == 2;
			this.count++;
			int s = Status.valueOf(context[1][1]).ordinal();
			this.status[s]++;
			dealPreAndNext(context, s, 0);
			dealPreAndNext(context, s, 2);

		}

		private void dealPreAndNext(String[][] context, int s, int index) {
			String[] pre = {"", ""};
			if(StringUtils.isNoneBlank(context[index])){
				pre = context[index];
				if(index == 2){
					int preS = Status.valueOf(pre[1]).ordinal();
					this.transfer[s][preS]++;
				}
			}
			String w = pre[0];
			HashMap<String, Double> cache = this.preStatus;
			if (index == 2) {
				cache = this.nextStatus;
			}
			String key = w + s;
			Double c = cache.get(key);
			if (c == null) {
				cache.put(key, 1.);
			} else {
				cache.put(key, ++c);
			}
		}

		public void cal() {
			for (int i = 0; i < this.status.length; i++) {
				this.status[i] = this.status[i] / this.count;
			}
			for (int i = 0; i < this.transfer.length; i++) {
				double count = 0;
				for (int j = 0; j < this.transfer[i].length; j++) {
					count += this.transfer[i][j];
				}
				for (int j = 0; j < this.transfer[i].length; j++) {
					this.transfer[i][j] = this.transfer[i][j] / count;
				}
				for (Entry<String, Double> entry : this.nextStatus.entrySet()) {
					if (entry.getKey().endsWith(String.valueOf(i))) {
						entry.setValue(entry.getValue() / count);
					}
				}
				for (Entry<String, Double> entry : this.preStatus.entrySet()) {
					if (entry.getKey().endsWith(String.valueOf(i))) {
						entry.setValue(entry.getValue() / count);
					}
				}
			}
		}
	}

	public static Status ordinal(int s) {
		switch (s) {
		case 0:
			return Status.S;
		case 1:
			return Status.B;
		case 2:
			return Status.M;
		case 3:
			return Status.E;
		default:
			return null;
		}
	}
	
	@Test
	public void testToString(){
		System.out.println("aaa" + Status.B);
	}

	public static void main(String[] args) {
		
		try {
			CRFDemo.create(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		viterbi("偷走别人17000元救命钱的姑娘又下手了！");
	}

	public static void viterbi(String sentence) {
		int len = sentence.length();
		int sl = Status.values().length;
		double[][] result = new double[len][sl];
		Arrays.asList(result).forEach(e -> Arrays.fill(e, 0.));
		int[][] path = new int[len][sl];
		String pre = null, cur = null, next = null;
		for (int i = 0; i < len; i++) {
			if (i == 0) {
				cur = sentence.substring(i, i + 1);
			} else {
				pre = cur;
				cur = next;
			}
			next = len > (i+1) ? sentence.substring(i + 1, i + 2) : null;
			Feature w = get(cur);
			assert w != null : "不存在的：" + cur;
			assert w.nextStatus != null : "不存在的：nextStatus";
			assert w.preStatus != null : "不存在的：preStatus";
			for (int j = 0; j < sl; j++) {
				result[i][j] = w.status[j];
				Double d = 0.;
				if(StringUtils.isNotBlank(pre)){
					Feature prew = get(pre);
					double max = 0;
					for(int m=0; m<sl; m++){
						double temp = prew.transfer[m][j] * result[i-1][m];
						if(temp > max ){
							max = temp;
							path[i][j] = m;
						}
					}
					result[i][j] += max;
					d = w.preStatus.get(pre + j);
				} else {
					d = w.preStatus.get(j);
				}
				result[i][j] += d == null ? 0. : d;
				if(StringUtils.isNotBlank(next)){
					d = w.nextStatus.get(next + j);
				} else {
					d = w.nextStatus.get(j);
				}
				result[i][j] += d == null ? 0. : d;
			}
		}
		
		for (int i = 0; i < len; i++) {
			System.out.print(sentence.charAt(i));
			System.out.print("[");
			for (int j = 0; j < sl; j++) {
				System.out.format("%f(%d) ", result[i][j], path[i][j]);
			}
			System.out.println("]");
		}
		print(sentence, path, result);
	}
	
	public static void print(String sentence, int[][] path, double[][] result){
		int index = 0;
		int len = result.length - 1;
		for (int i = 0, l = result[len].length; i < l - 1; i++) {
			if(result[len][index] < result[len][i + 1]){
				index = i + 1;
			}
		}
		ArrayList<Integer> order = new ArrayList<Integer>();
		order.add(index);
		for (int i = len; i > 0; i--) {
			order.add(0, path[i][index]);
			index = path[i][index];
		}
		System.out.println(order.toString());
		for (int i = 0; i < sentence.length(); i++) {
			if(order.get(i) == Status.S.ordinal()){
				System.out.print(" ");
				System.out.print(sentence.charAt(i));
				System.out.print(" ");
			} else if(order.get(i) == Status.B.ordinal()){
				System.out.print(" ");
				System.out.print(sentence.charAt(i));
			} else if(order.get(i) == Status.M.ordinal()){
				System.out.print(sentence.charAt(i));
			} else if(order.get(i) == Status.E.ordinal()){
				System.out.print(sentence.charAt(i));
				System.out.print(" ");
			}
		}
		System.out.println();
	}
	

	public static CRFDemo create(boolean reload) throws IOException {
		if (demo == null) {
			demo = new CRFDemo();
			String ic = "msr_training.utf8.ic";
			String path = CRFDemo.class.getClassLoader().getResource(ic).getPath();
			path = path.substring(0, path.length() - ic.length());
			String cachePath = path + "obj";
			if(reload){
				demo.load(ic);
				writeCache(words, cachePath);
			} else {
				readCache("obj", CRFDemo.demo, "words");
				System.out.println(words.keySet().toString());
			}
			
		}
		return demo;
	}

	private static void writeCache(Object obj, String path){
		
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));){
			out.writeObject(obj);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void readCache(String path, Object target, String fieldName){
		try (ObjectInputStream in = new ObjectInputStream(CRFDemo.class.getClassLoader().getResourceAsStream(path))){
			System.out.println(path);
			String[] fs = fieldName.split("\\.");
			Class<?> clazz = target.getClass();
			Field field;
			for (int i = 0; i < fs.length - 1; i++) {
				field = clazz.getDeclaredField(fs[i]);
				field.setAccessible(true);
				Object obj = field.get(target);
				if(obj == null){
					obj = field.getClass().newInstance();
					field.set(target, obj);
				}
				target = obj;
			}
			field = clazz.getDeclaredField(fs[fs.length - 1]);
			field.setAccessible(true);
			field.set(target, in.readObject());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void load(String path) throws IOException {
		try (InputStreamReader reader = new InputStreamReader(
				this.getClass().getClassLoader().getResourceAsStream(path))) {
			
			try (BufferedReader in = new BufferedReader(reader)) {
				try(InputStreamReader r2 = new InputStreamReader(
						this.getClass().getClassLoader().getResourceAsStream("msr_training.utf8"))){
					try(BufferedReader in2 = new BufferedReader(r2)){
						String l2;
						while((l2 = in2.readLine()) != null){
							String line = null;
							String[][] context = new String[3][2];
							boolean isfirst = true;
							int count = 0;
							l2 = l2.replaceAll("\\s", "");
							while (count == l2.length() || (line = in.readLine()) != null) {
								if(count == l2.length())
									break;
								count++;
								if(line == null || !line.matches("^\\S+\\|[BSME]$")) continue;
								for (int i = 0; i < 2; i++) {
									if (context[i + 1] != null)
										context[i] = context[i + 1];
								}
								int index = line.lastIndexOf("|");
								context[2] = new String[]{line.substring(0, index), line.substring(index+1)};
								Arrays.asList(context).forEach(e -> StringUtils.stripAll(e));
								System.out.println(count + Arrays.toString(context[2]));
								if (!isfirst) {
									dealAndCount(context);
								} else {
									isfirst = false;
								}
							}
							if(l2.length() > 0){
								context[0] = context[1];
								context[1] = context[2];
								context[2] = null;
								dealAndCount(context);
							}
						}
					}
				}
			}
		}
		calculate();
	}

	private static CRFDemo demo;
	private static HashMap<String, Feature> words = new HashMap<String, Feature>();

	private void dealAndCount(String[][] context) {
		assert context.length == 3 && ArrayUtils.isNotEmpty(context[1]);
		String word = StringUtils.trimToEmpty(context[1][0]);
		Feature feature = words.get(word);
		if (feature == null) {
			feature = new Feature(context);
			words.put(word, feature);
		} else {
			feature.count(context);
		}
	}

	private void calculate() {
		System.out.println(words.keySet().toString());
		for (Entry<String, Feature> entry : words.entrySet()) {
			entry.getValue().cal();
		}
	}

	public static Feature get(String word) {
		return words.get(word);
	}

}
