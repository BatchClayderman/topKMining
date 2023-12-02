package ttfe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;


public class AlgoTTFE
{
	/** ��Ҫ���� **/
	public static final int defaultK = 10; // Ĭ��ֵ
	public static final double defaultAlpha = 0.5, defaultBeta = 0.5; // Ĭ��ֵ
	public static final boolean defaultPrint = false;
	int topK = defaultK; // top-k
	double alpha = defaultAlpha, beta = defaultBeta; // ����
	Double delta = null;
	boolean isPrint = defaultPrint; // ������ؿ���
	
	private String inputFile, outputFile;
	private ArrayList<Transaction> transactions = new ArrayList<>(); // ÿһ��
	private LinkedHashMap<Integer, Double> TWTF = new LinkedHashMap<>(); // ����ÿ�� item ������ item �� transaction �� TTF ���
	private LinkedHashMap<Integer, Double> RETF = new LinkedHashMap<>(); // ÿ�� item �ĺ�
	private Table LETF;
	private Double[] lifu_e = null;
	private Double[] lb_lifu = null;
	
	double startTimestamp = 0, endTimestamp = 0;
	double maxMemory = -1; // �洢����ڴ�
	int htfeCount = 0;
	Double minTTFEValue = null;
	
	
	public AlgoTTFE()
	{
		this.topK = defaultK;
		this.alpha = defaultAlpha;
		this.beta = defaultBeta;
		this.isPrint = defaultPrint;
	}
	public AlgoTTFE(int topK)
	{
		this.topK = topK;
		this.alpha = defaultAlpha;
		this.beta = defaultBeta;
		this.isPrint = defaultPrint;
	}
	public AlgoTTFE(int topK, double alpha, double beta)
	{
		this.topK = topK;
		this.alpha = alpha;
		this.beta = beta;
		this.isPrint = defaultPrint;
	}
	public AlgoTTFE(int topK, double alpha, double beta, boolean isPrint)
	{
		this.topK = topK;
		this.alpha = alpha;
		this.beta = beta;
		this.isPrint = isPrint;
	}
    
    
    
	/** �Ӻ��� **/
	public static void printArray(Object object, boolean root)
	{
		if (null == object)
			System.out.print("null");
		else if (object.getClass().isArray())
		{
			int length = Array.getLength(object);	
			if (length <= 0) // Empty array
				System.out.println("[]");
			else
			{
				System.out.print("[");
				printArray(Array.get(object, 0), false);
				for (int i = 1; i < length; ++i)
				{
					System.out.print(", ");
					printArray(Array.get(object, i), false);
				}
				System.out.print("]");
			}
		}
		else
			System.out.print(object);
		if (root)
			System.out.println();
		return;
	}
	
	public static void printArray(Object object)
	{
		printArray(object, true);
		return;
	}
	
	
	
	/** ���� **/
	public class TF extends AlgoTTFE
	{
		double threat = 0, frequency = 0, tf = 0;
		public TF(double frequency, double utility, double alpha, double beta)
		{
			this.threat = frequency;
			this.frequency = utility;
			this.tf = this.alpha * this.threat + this.beta * this.frequency;
		}
	}
	
	public class Transaction extends AlgoTTFE // ĳһ��
	{
		public class TreeNode
		{
			ArrayList<Integer> series = new ArrayList<>(); // ����
			double afu, efu;
			public TreeNode(ArrayList<Integer> series, double afu, double efu)
			{
				this.series = series;
				this.afu = afu;
				this.efu = efu;
			}
			public String toString()
			{
				String sRet = "[" + this.series.get(0);
				for (int i = 1; i < this.series.size(); ++i)
					sRet += ", " + this.series.get(i);
				sRet += "]:[" + this.afu + ", " + this.efu + "]";
				return sRet;
			}
		}
		
		int tid = 0;
		LinkedHashMap<Integer, TF> items = new LinkedHashMap<>();
		Double tfu = null, max_value = null;
		ArrayList<TreeNode> treeNodes = new ArrayList<>(); // һ�������ɿ������������һ�Ĳ��ƽ����
		ArrayList<Integer> results = new ArrayList<>();
		public Transaction(int tid, double alpha, double beta)
		{
			this.tid = tid;
			this.alpha = alpha;
			this.beta = beta;
		}
		public boolean contains(Integer item)
		{
			return this.items.containsKey(item);
		}
		public boolean put(Integer item, TF tf)
		{
			if (this.contains(item))
				return false;
			items.put(item, tf);
			return true;
		}
		public boolean remove(Integer item)
		{
			if (!this.contains(item))
				return false;
			items.remove(item);
			return true;
		}
		public double update(double tfu)
		{
			this.tfu = tfu;
			return this.tfu;
		}
		public double update()
		{
			this.tfu = (double)0; // ����
			Set<Entry<Integer, TF>> set = this.items.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				TF tmp = (TF)entry.getValue();
				this.tfu += this.alpha * tmp.threat + this.beta * tmp.frequency;
			}
			return this.tfu;
		}
		public int index(int item)
		{
			int tmp = -1;
			Set<Entry<Integer, TF>> set = this.items.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				++tmp;
				Entry<Integer, TF> entry = iterator.next();
				if (entry.getKey() == item)
					return tmp;
			}
			return -1;
		}
		public boolean isSeries(ArrayList<Integer> lists)
		{
			int tmp = this.index(lists.get(0));
			if (-1 == tmp) // �Ҳ���
				return false;
			for (int i = 1; i < lists.size(); ++i)
				if (++tmp != this.index(lists.get(i)))
					return false;
			return true;
		}
		public void addNode(ArrayList<Integer> series, double afu, double efu)
		{
			ArrayList<Integer> new_series = new ArrayList<>();
			for (int i = 0; i < series.size(); ++i)
				new_series.add(series.get(i));
			TreeNode tmpNode = new TreeNode(new_series, afu, efu);
			this.treeNodes.add(tmpNode);
			return;
		}
		
		public double getAfuPerTransaction(ArrayList<Integer> series)
		{
			double tranAFU = 0;
			for (int i = 0; i < series.size(); ++i)
				if (this.items.containsKey(series.get(i)))
					tranAFU += this.items.get(series.get(i)).tf;
				else
					return 0;
			return tranAFU;
		}
		public ArrayList<Integer> getMissingNodePerTransaction(ArrayList<Integer> series)
		{
			ArrayList<Integer> missingNodes = new ArrayList<>();
			if (this.getAfuPerTransaction(series) <= 0) // ��ǰ���в��ǺϷ�������
				return missingNodes;
			Set<Entry<Integer, TF>> set = this.items.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				if ((Integer)entry.getKey().intValue() == series.get(0).intValue()) // �ҵ���һ��ƥ�������
				{
					for (int cnt = 1; cnt < series.size(); ++cnt) // ����������������Բ���  item �еĲ�����ƥ����
					{
						entry = iterator.next();
						if ((Integer)entry.getKey().intValue() != series.get(cnt).intValue())
							missingNodes.add(series.get(cnt));
					}
					break;
				}
			}
			return missingNodes;
		}
		public void printTree()
		{
			System.out.print(this.tid + " - {");
			if (!this.treeNodes.isEmpty())
			{
				System.out.print(this.treeNodes.get(0));
				for (int i = 1; i < this.treeNodes.size(); ++i)
					System.out.print(", " + this.treeNodes.get(i));
			}
			System.out.println("}");
			return;
		}
		public String getResult()
		{
			String sRet = this.tid + " - {";
			if (!this.results.isEmpty())
			{
				sRet += this.results.get(0);
				for (int i = 1; i < this.results.size(); ++i)
					sRet += ", " + this.results.get(i);
			}
			sRet += "} - " + this.max_value;
			return sRet;
		}
		public String getString(boolean[] options)
		{
			if (null == this.tfu)
				this.update();
			String sRet = this.tid + " - {";
			Set<Entry<Integer, TF>> set = this.items.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				TF tmp = (TF)entry.getValue();
				String shows = (
					(options[0] ? tmp.threat + ", " : "")
					+ (options[1] ? tmp.frequency + ", " : "")
					+ (options[2] ? tmp.tf + ", " : "")
				);
				if (shows.endsWith(", "))
					shows = shows.substring(0, shows.length() - 2); // ��������Ķ���
				sRet += (Integer)entry.getKey() + ":[" + shows + "]" + (iterator.hasNext() ? ", " : "} - " + this.tfu);
			}
			return sRet;
		}
		public String toString()
		{
			boolean[] options = {true, true, true};
			return this.getString(options);
		}
	}
	
	public class Table extends AlgoTTFE
	{
		String name; // ����
		int[] columns; // �Ϸ�����
		int[] index; // �������
		int[] series; // ����
		double[][] values;
		public Table(int[] columns, int[] index, double[][] values, String name)
		{
			this.columns = columns;
			this.index = index;
			this.values = values;
			this.series = new int[index.length + 1];
			this.series[0] = index[0];
			for (int i = 0; i < columns.length; ++i)
				this.series[i + 1] = columns[i];
			this.name = name;
		}
		public Table(int[] series, double[][] values, String name)
		{
			this.series = series;
			this.values = values;
			this.columns = new int[series.length - 1];
			this.index = new int[series.length - 1];
			for (int cnt = 0; cnt < series.length; ++cnt)
			{
				if (0 == cnt)
					index[cnt] = series[cnt];
				else if (series.length - 1 == cnt)
					columns[cnt - 1] = series[cnt];
				else
				{
					index[cnt] = series[cnt];
					columns[cnt - 1] = series[cnt];
				}
			}
			this.name = name;
		}
		private int getColumnsIndex(int columns)
		{
			for (int i = 0; i < this.columns.length; ++i)
				if (this.columns[i] == columns)
					return i;
			return -1;
		}
		private int getIndexIndex(int index)
		{
			for (int i = 0; i < this.index.length; ++i)
				if (this.index[i] == index)
					return i;
			return -1;
		}
		public boolean addValueByID(int index_id, int columns_id, double value)
		{
			if (index_id < 0 || columns_id < 0 || index_id >= this.index.length || columns_id >= this.columns.length)
				return false;
			this.values[index_id][columns_id] += value;
			return true;
		}
		public boolean addValueByName(int index_name, int columns_name, double value)
		{
			int columnsIndex = this.getColumnsIndex(columns_name), indexIndex = this.getIndexIndex(index_name);
			if (-1 == columnsIndex || -1 == indexIndex)
				return false;
			this.values[indexIndex][columnsIndex] += value;
			return true;
		}
		private ArrayList<ArrayList<Integer>> combine_series(int m, int n, int controller[])
		{
			ArrayList<ArrayList<Integer>> combines = new ArrayList<>();
			if (1 == m) // �ݹ����
			{
				for (int i = m; i <= n; ++i)
				{
					ArrayList<Integer> sub_combine = new ArrayList<>();
					controller[0] = i;
					for (int j = 0; j < controller.length; ++j)
						sub_combine.add(this.series[controller[j] - 1]);
					combines.add(sub_combine);
				}
				return combines;
			}
			for (int i = m; i <= n; ++i)
			{
				controller[m - 1] = i;
				combines.addAll(this.combine_series(m - 1, i - 1, controller));
			}
			return combines;
		}
		public ArrayList<ArrayList<Integer>> combine_series(int n)
		{
			ArrayList<ArrayList<Integer>> combines = new ArrayList<>();
			for (int i = 2; i <= n; ++i)
			{
				int[] controller = new int[i]; // ���ڿ������
				combines.addAll(this.combine_series(i, n, controller));
			}
			return combines;
		}
		public ArrayList<Integer> getSubSeries(int p, int q, boolean ht)
		{
			ArrayList<Integer> array = new ArrayList<>();
			boolean isAdd = false;
			for (Integer element : this.series)
			{
				if (element.intValue() == p) // ��ʼ�ػ�
					isAdd = true;
				else if (element.intValue() == q) // ֹͣ�ػ�
					break;
				else if (isAdd)
					array.add(element);
			}
			if (ht)
			{
				array.add(0, p);
				array.add(q);
			}
			return array;
		}
		public String toString()
		{
			String sRet = "Series: " + this.series[0];
			for (int i = 1; i < this.series.length; ++i)
				sRet += " -> " + this.series[i];
			sRet += "\n" + this.name;
			for (int c : this.columns)
				sRet += "\t" + c;
			sRet += "\n";
			for (int i = 0; i < this.index.length; ++i)
			{
				sRet += this.index[i];
				for (int j = 0; j < this.values[i].length; ++j)
					sRet += "\t" + this.values[i][j];
				sRet += "\n";
			}
			return sRet;
		}
	}
	
	
	
    /** ttfe ������ **/
	/* ��ȡ���� */
	private boolean initTTFE() throws IOException
	{
		BufferedReader myInput = null;
        String thisLine;
        int tid = 0;
        boolean bRet = true;
        try
        {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(this.inputFile))));
            while ((thisLine = myInput.readLine()) != null) // ���ж�ȡ
            {
                if (
                        thisLine.isEmpty() == true || thisLine.charAt(0) == '#'
                                || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@'
                                || (thisLine.length() > 1 && thisLine.charAt(0) == '/' && thisLine.charAt(1) == '/')
                ) // �������кͱ�ע����
                    continue;
                // Item : Threat : Frequency : TTF
                thisLine = thisLine.replace("\t", " "); // ȥ������
                while (thisLine.contains("  ")) // ȥ������ո�
                	thisLine = thisLine.replace("  ", " ");
                thisLine = thisLine.replace(" : ", ":"); // ȥ��ð��ǰ��Ŀո�
                while (thisLine.startsWith(" ")) // ȥ����ͷ�Ŀո�
                	thisLine = thisLine.substring(1);
                while (thisLine.endsWith(" ")) // ȥ����ĩ�Ŀո�
                	thisLine = thisLine.substring(0, thisLine.length() - 1);
                
                if (thisLine.toLowerCase().startsWith("topk = ")) // ���� topK ����
                {
                	this.topK = Integer.parseInt(thisLine.substring(7));
                	if (this.topK < 1)
                		throwException("\nValue of the input topK should be greater than 0. \n" + thisLine);
                	continue;
                }
                else if (thisLine.toLowerCase().startsWith("alpha = ")) // ���� alpha ����
                {
                	this.alpha = Double.parseDouble(thisLine.substring(8));
                	if (this.alpha < 0 || this.alpha > 1)
                		throwException("\nValue of the input alpha should be in range [0, 1]. \n" + thisLine);
                	continue;
                }
                else if (thisLine.toLowerCase().startsWith("beta = ")) // ���� beta ����
                {
                	this.alpha = Double.parseDouble(thisLine.substring(7));
                	if (this.beta < 0 || this.beta > 1)
                		throwException("\nValue of the input beta should be in range [0, 1]. \n" + thisLine);
                	continue;
                }
                
                Transaction transaction = new Transaction(tid++, this.alpha, this.beta);
                String[] split = thisLine.split(":");
                String[] item_str = split[0].split(" ");
                String[] frequency_str = split[1].split(" ");
                String[] utility_str = split[2].split(" ");
                Double tfu = null;
                try // ���Խ��м���
                {
                	tfu = Double.parseDouble(split[3]);
                }
                catch(Throwable e)
                {
                	tfu = null; // ����ֱ���ÿ�
                }
                
                for (int i = 0; i < item_str.length; ++i) // ���齨�� TID
                {
                    Integer item = Integer.parseInt(item_str[i]);
                    TF tf = new TF(Double.parseDouble(frequency_str[i]), Double.parseDouble(utility_str[i]), this.alpha, this.beta);
                    transaction.put(item, tf);
                }
                
                if (null != tfu && transaction.update() != tfu) // ��ֹ TTF ����TTF ���ݼ��飩
                	throwException("\nValue of the input TTF is not the same as calculated. \n" + thisLine + " - " + transaction.update());
                transactions.add(transaction);
            }
            
            if (isPrint)
            {
            	System.out.println("/************************************************** TTFE - initTTFE() **************************************************/");
            	System.out.println("\t\t\t\talpha = " + this.alpha + "\t\t\t\t\tbeta = " + this.beta + "\t\t\t\t");
            	for (Transaction transaction : transactions)
            	System.out.println(transaction);
            	System.out.println();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            bRet = false;
        }
        finally
        {
            if (myInput != null)
                myInput.close();
        }
        return bRet;
	}
	
	/* ���� TWTF */
	private void calcTWTF()
	{
		for (Transaction transaction : this.transactions)
		{
			LinkedHashMap<Integer, TF> items = transaction.items;
			Set<Entry<Integer, TF>> set = items.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				Integer key = (Integer)(entry.getKey());
				this.TWTF.put(key, (this.TWTF.containsKey(key) ? this.TWTF.get(key) : 0) + transaction.tfu);
			}
		}
		
		if (this.isPrint)
		{
			System.out.println("/************************************************** TTFE - calcTWTF() **************************************************/");
			System.out.printf("TWTF: {");
			Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.print((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : "}\n\n"));
			}
		}
		return;
	}
	
	/* ���� TWTF */
	private void sortedTWTF() // ѡ�����򣨴�С��������
	{
		LinkedHashMap<Integer, Double> new_TWTF = new LinkedHashMap<>(); // �����
		for (int i = 0; i < this.TWTF.size(); ++i)
		{
			Integer min_key = null;
			Double min_value = null;
			Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				Integer currentKey = (Integer)entry.getKey();
				Double currentValue = (Double)entry.getValue();
				if (
						!new_TWTF.containsKey(currentKey) // �ų�������Ŀ�
						&& (null == min_value || min_value > currentValue) // ��λ��Сֵ
				) // ������Сֵ
				{
					min_key = currentKey;
					min_value = currentValue;
				}
			}
			new_TWTF.put(min_key, min_value);
		}
		this.TWTF = new_TWTF;
		
		if (this.isPrint)
		{
			System.out.println("/************************************************* TTFE - sortedTWTF() *************************************************/");
			System.out.printf("TWTF(UP): {");
			Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.printf((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : "}\n\n"));
			}
		}
		return;
	}
	
	/* ���� RETF */
	private void calcRETF()
	{
		for (Transaction transaction : this.transactions)
		{
			LinkedHashMap<Integer, TF> items = transaction.items;
			Set<Entry<Integer, TF>> set = items.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				Integer key = (Integer)(entry.getKey());
				TF value = (TF)(entry.getValue());
				double values = this.alpha * value.threat + this.beta * value.frequency;
				this.RETF.put(key, (this.RETF.containsKey(key) ? this.RETF.get(key) : 0) + values);
			}
		}
		
		if (this.isPrint)
		{
			System.out.println("/************************************************** TTFE - calcRETF() **************************************************/");
			System.out.printf("RETF: {");
			Set<Entry<Integer, Double>> set = this.RETF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.printf((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : "}\n\n"));
			}
		}
		return;
	}
	
	/* ���� RETF */
	private void sortedRETF() // ѡ�����򣨴Ӵ�С����
	{
		LinkedHashMap<Integer, Double> new_RETF = new LinkedHashMap<>(); // �����
		Integer max_key = null;
		Double max_value = null;
		for (int i = 0; i < this.RETF.size();)
		{
			max_key = null; // ����
			max_value = null;
			Set<Entry<Integer, Double>> set = this.RETF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				Integer currentKey = (Integer)entry.getKey();
				Double currentValue = (Double)entry.getValue();
				if (
						!new_RETF.containsKey(currentKey) // �ų�������Ŀ�
						&& (null == max_value || max_value < currentValue) // ��λ���ֵ
				) // �������ֵ
				{
					max_key = currentKey;
					max_value = currentValue;
				}
			}
			new_RETF.put(max_key, max_value);
			if (++i == this.topK) // ��ֵ����
				delta = new Double(max_value);
		}
		this.RETF = new_RETF;
		
		if (this.isPrint)
		{
			System.out.println("/************************************************* TTFE - sortedRETF() *************************************************/");
			System.out.printf("RETF(DOWN): {");
			Set<Entry<Integer, Double>> set = this.RETF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.printf((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : "}\n"));
			}
			System.out.println("delta = " + delta + "\n");
		}
		return;
	}
	
	/* topK ��֦ */
	private void prunItem()
	{
		LinkedHashMap<Integer, Double> OTWTF = new LinkedHashMap<>();
		Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
		Iterator<Entry<Integer, Double>> iterator = set.iterator();
		while (iterator.hasNext())
		{
			Entry<Integer, Double> entry = iterator.next();
			if (null == this.delta || (double)entry.getValue() >= this.delta)
				OTWTF.put(entry.getKey(), entry.getValue());
			else
				for (int i = 0; i < this.transactions.size(); ++i)
					this.transactions.get(i).remove(entry.getKey());
		}
		this.TWTF = OTWTF;
		
		if (this.isPrint)
		{
			System.out.println("/************************************************** TTFE - prunItem() **************************************************/");
			System.out.println("delta:" + this.delta + "\t\ttopK: " + this.topK);
			System.out.println("OriginalSize: " + this.RETF.size() + "\t\tCurrentSize: " + this.TWTF.size() + "\t\tCut: " + (this.RETF.size() - this.TWTF.size()));
			set = this.TWTF.entrySet();
			iterator = set.iterator();
			System.out.print("Pruned TWTF: {");
			if (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				this.delta = entry.getValue(); // ��ֵ
				iterator = set.iterator();
			}
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.printf((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : "}\nPruned TTFE: \n"));
			}
			for (Transaction transaction : transactions)
	        	System.out.println(transaction);
			System.out.println();
		}
		return;
	}
	
	/* ���� TTFE */
	private void sortedTTFE()
	{
		ArrayList<Transaction> new_transactions = new ArrayList<>();
		for (Transaction transaction : this.transactions)
		{
			Transaction new_transaction = new Transaction(transaction.tid, transaction.alpha, transaction.beta);
			Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				if (transaction.contains((Integer)entry.getKey()))
					new_transaction.put(entry.getKey(), transaction.items.get(entry.getKey()));
			}
			new_transactions.add(new_transaction);
		}
		this.transactions = new_transactions;
		
		if (this.isPrint)
		{
			System.out.println("/************************************************* TTFE - sortedTTFE() *************************************************/");
	        System.out.println("\t\t\t\talpha = " + this.alpha + "\t\t\t\t\tbeta = " + this.beta + "\t\t\t\t");
	        for (Transaction transaction : transactions)
	        	System.out.println(transaction);
	        System.out.println();
		}
		return;
	}
	
	/* ���� Table */
	private void calcTable()
	{
		int[] columns = new int[this.TWTF.size() - 1];
		int[] index = new int[this.TWTF.size() - 1];
		double[][] values = new double[this.TWTF.size() - 1][this.TWTF.size() - 1];
		Set<Entry<Integer, Double>> set_twfu = this.TWTF.entrySet();
		Iterator<Entry<Integer, Double>> iterator_twfu = set_twfu.iterator();
		for (int cnt = 0; iterator_twfu.hasNext(); ++cnt)
		{
			Entry<Integer, Double> entry = iterator_twfu.next();
			if (0 == cnt)
				index[cnt] = (Integer)entry.getKey();
			else if (this.TWTF.size() - 1 == cnt)
				columns[cnt - 1] = (Integer)entry.getKey();
			else
			{
				index[cnt] = (Integer)entry.getKey();
				columns[cnt - 1] = (Integer)entry.getKey();
			}
		}
		this.LETF = new Table(columns, index, values, "LETF");
		
		for (Transaction transaction : this.transactions) // ÿ�� transaction
		{
			Set<Entry<Integer, TF>> set_items = transaction.items.entrySet();
			Iterator<Entry<Integer, TF>> iterator_item = set_items.iterator();
			ArrayList<Integer> item_series = new ArrayList<>(); // һ�е�����
			while (iterator_item.hasNext()) // ÿ�� item
			{
				Entry<Integer, TF> entry_item = iterator_item.next();
				item_series.add((Integer)entry_item.getKey());
			}
			for (int i = 0; i < this.LETF.series.length - 1; ++i)
			{
				int p = this.LETF.series[i]; // ������ͷ
				if (!transaction.items.containsKey(p)) // �Ҳ���ֱ������
					continue;
				ArrayList<Integer> sub_series = new ArrayList<>(); // ������
				sub_series.add(p);
				double pq_value = transaction.items.get(p).tf;
				for (int j = i + 1; j < this.LETF.series.length; ++j)
				{
					int q = this.LETF.series[j]; // ������β
					if (!transaction.items.containsKey(q)) // �Ҳ���ֱ���ж�
						break;
					sub_series.add(q);
					pq_value += transaction.items.get(q).tf; // �����к�
					if (transaction.isSeries(sub_series))
						this.LETF.addValueByName(p, q, pq_value);
					else // һ��ѭ���Ѳ�����
						break; // ����һ��ѭ��
				}
			}
		}
		
		if (this.isPrint)
		{
			System.out.println("/************************************************** TTFE - calcTable() **************************************************/");
			System.out.println(this.LETF);
		}
		return;
	}
	
	/* LETF ��ֵ�������� 1 */
	public void raiseThreshold_LETF_E()
	{
		this.lifu_e = new Double[this.topK];
		for (int cnt = 0; cnt < this.topK; ++cnt) // �ÿ�
			this.lifu_e[cnt] = null;
		for (int i = 0; i < this.LETF.values.length; ++i)
			for (int j = 0; j < this.LETF.values[i].length; ++j)
				for (int cnt = 0; cnt < this.topK; ++cnt)
					if (null == this.lifu_e[cnt] || this.LETF.values[i][j] > this.lifu_e[cnt])
					{
						for (int k = this.topK - 1; k > cnt; --k) // ����
							this.lifu_e[k] = this.lifu_e[k - 1];
						this.lifu_e[cnt] = this.LETF.values[i][j];
						break;
					}
		for (int i = this.lifu_e.length - 1; i > -1; --i)
			if (null != this.lifu_e[i])
			{
				this.delta = this.lifu_e[i];
				break;
			}
		
		if (this.isPrint)
		{
			System.out.println("/******************************************** TTFE - raiseThreshold_LETF_E() ********************************************/");
			printArray(this.lifu_e);
			System.out.println("delta = " + this.delta + "\n");
		}
		return;
	}
	
	/* LETF ��ֵ�������� 2 */
	public void raiseThreshold_LB_LETF()
	{
		this.lb_lifu = new Double[this.topK];
		for (int cnt = 0; cnt < this.topK; ++cnt) // �ÿ�
			this.lb_lifu[cnt] = null;
		for (int i = 0; i < this.LETF.values.length; ++i)
			for (int j = 0; j < this.LETF.values[i].length; ++j)
			{
				int p = this.LETF.index[i], q = this.LETF.columns[j];
				ArrayList<Integer> array = this.LETF.getSubSeries(p, q, false); // ��ȡ i��j ֮������� 
				double tmp_value = this.LETF.values[i][j];
				for (int m = 0; m < 3 && m < array.size(); ++m)
				{
					tmp_value -= this.RETF.get(array.get(m));
					if (tmp_value < this.delta) // ������ֵ
						break;
					for (int cnt = 0; cnt < this.topK; ++cnt)
					{
						if (null == this.lb_lifu[cnt] || tmp_value > this.lb_lifu[cnt])
						{
							for (int n = this.topK - 1; n > cnt; --n) // ����
								this.lb_lifu[n] = this.lb_lifu[n - 1];
							this.lb_lifu[cnt] = tmp_value;
							break;
						}
					}
				}
			}
		for (int i = this.lb_lifu.length - 1; i > -1; --i)
			if (null != this.lb_lifu[i])
			{
				this.delta = this.lb_lifu[i];
				break;
			}
		
		if (this.isPrint)
		{
			System.out.println("/******************************************** TTFE - raiseThreshold_LB_LETF() ********************************************/");
			printArray(this.lb_lifu);
			System.out.println("delta = " + this.delta + "\n");
		}
		return;
	}
		
	/* ������ */
	public void calcTree()
	{
		for (ArrayList<Integer> series : this.LETF.combine_series(this.LETF.series.length))
			for (Transaction tran : this.transactions)
			{
				double afu = 0;
				for (Transaction saction : this.transactions) // ���
					afu += saction.getAfuPerTransaction(series);
				ArrayList<Integer> node_efu = tran.getMissingNodePerTransaction(series);
				double _efu = 0;
				for (int i = 0; i < node_efu.size(); ++i)
					_efu += this.RETF.get(node_efu.get(i));
				tran.addNode(series, afu, afu - _efu);
			}
		
		if (this.isPrint)
		{
			System.out.println("/*************************************************** TTFE - calcTree() ***************************************************/");
			for (Transaction transaction : this.transactions)
				transaction.printTree();
			System.out.println();
		}
		return;
	}
	
	/* ��֦ */
	public void prunTree()
	{
		for (Transaction transaction : this.transactions)
			for (int i = transaction.treeNodes.size() - 1; i > -1; --i)
				if (transaction.treeNodes.get(i).afu + transaction.treeNodes.get(i).efu < this.delta)
					transaction.treeNodes.remove(i);
		
		if (this.isPrint)
		{
			System.out.println("/*************************************************** TTFE - prunTree() ***************************************************/");
			System.out.println("delta = " + this.delta);
			for (Transaction transaction : this.transactions)
				transaction.printTree();
			System.out.println();
		}
		return;
	}
	
	/* ��� */
	public void getResults() throws IOException
	{
		for (Transaction transaction : this.transactions)
		{
			for (int i = 0; i < transaction.treeNodes.size(); ++i)
				if (null == transaction.max_value || transaction.treeNodes.get(i).efu > transaction.max_value)
				{
					transaction.results = transaction.treeNodes.get(i).series;
					transaction.max_value = transaction.treeNodes.get(i).efu;
				}
		}
		
		if (this.isPrint)
		{
			System.out.println("/************************************************** TTFE - getResults() **************************************************/");
			File file = new File(this.outputFile);
			FileWriter fw = new FileWriter(file);
			fw.write("alpha = " + this.alpha + "\t\tbeta = " + this.beta + "\ntopK = " + this.topK + "\t\tdelta = " + this.delta);
			for (Transaction transaction : this.transactions)
			{
				if (null == this.minTTFEValue || this.minTTFEValue > transaction.max_value)
					this.minTTFEValue = transaction.max_value;
				this.htfeCount += transaction.results.size();
				String tmpResult = transaction.getResult();
				System.out.println(tmpResult);
				fw.write("\n" + tmpResult);
			}
			fw.close();
			System.out.println("\n");
		}
		return;
	}
	
    /* ������ں��� */
    public boolean runAlgorithm(String inputFile, String outputFile) throws IOException
    {
    	this.inputFile = inputFile;
    	this.outputFile = outputFile;
    	
    	startTimestamp = System.currentTimeMillis();
    	if (!(this.initTTFE() && this.checkMemory()))
    		return false;
    	this.calcTWTF();this.checkMemory();
    	this.sortedTWTF();this.checkMemory();
    	this.calcRETF();this.checkMemory();
    	this.sortedRETF();this.checkMemory();
    	this.prunItem();this.checkMemory();
    	this.sortedTTFE();this.checkMemory();
    	this.calcTable();this.checkMemory();
    	this.raiseThreshold_LETF_E();this.checkMemory();
    	this.raiseThreshold_LB_LETF();this.checkMemory();
    	this.calcTree();this.checkMemory();
    	this.prunTree();this.checkMemory();
    	this.getResults();this.checkMemory();
    	endTimestamp = System.currentTimeMillis();
    	return true;
    }
    
    
    
    /** ���ܺ��� **/
    private boolean checkMemory()
	{
        double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d / 1024d;
        if (currentMemory > maxMemory)
        {
        	maxMemory = currentMemory;
            return true;
        }
        else
        	return false;
    }
    
    private void throwException(String description) // ���쳣����
    {
    	ArithmeticException e = new ArithmeticException(description);
    	throw e;
    }
	
	public void printStats() throws IOException
	{
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.00");
        System.out.println("============  TTFE ALGORITHM - STATS  ============");
        System.out.println("\ttopK: " + this.topK);
        System.out.println("\talpha: " + this.alpha);
        System.out.println("\tbeta: " + this.beta);
        System.out.println("\tdelta: " + this.delta);
        System.out.println("\tTotal time: " + (endTimestamp - startTimestamp) / 1000.0 + " s");
        System.out.println("\tMax memory: " + df.format(maxMemory) + " MB");
        System.out.println("\tHTFEs count: " + this.htfeCount);
        System.out.println("\tMinimum threat-frequency: " + this.minTTFEValue);
        File f = new File(inputFile);
        String tmp = f.getName();
        tmp = tmp.substring(0, tmp.lastIndexOf('.'));
        System.out.println("\tDataset: " + tmp);
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        System.out.println("\tEnd time: " + timeStamp);
        System.out.println("==================================================");
        return;
    }
}