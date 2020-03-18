package DSJ;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.MyBloatFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import CustomWritables.DTJrPairElement;
import CustomWritables.DTJrPointElement;
import java.net.URI;


public class DSJDriver {

	public static String hostname = new String();
	public static String dfs_port = new String();
	public static String rm_port = new String();
	public static String workspace_dir = new String();
	public static String input_dir = new String();
	public static String output_dir = new String();
	public static String dtjmr_subtraj_dir = new String();

	public static int nof_reducers;
	
	public static int e_sp_method;
	public static double epsilon_sp;
	public static int epsilon_t;
	public static int dt;


    public static void main(String[] args) throws Exception {
    	
       
     	hostname = args[0];
     	dfs_port = args[1];
     	rm_port = args[2];
     	workspace_dir = args[3];
     	input_dir = args[4];
     	output_dir = args[5];
     	
     	nof_reducers = Integer.parseInt(args[6]);
     	
     	e_sp_method = Integer.parseInt(args[7]);
     	epsilon_sp = Double.parseDouble(args[8]);

     	epsilon_t = Integer.parseInt(args[9]);
     	dt = Integer.parseInt(args[10]);

     	final String job_description = args[11];
        
   	
		Configuration conf = new Configuration();

     	FileSystem fs = FileSystem.get(conf);
        
       	conf.set("hostname", hostname);
       	conf.set("dfs_port", dfs_port);
       	conf.set("rm_port", rm_port);
       	conf.set("workspace_dir", workspace_dir);
       	conf.set("input_dir", input_dir);
       	conf.set("output_dir", output_dir);
       	conf.setInt("nof_reducers", nof_reducers);
       	conf.setInt("e_sp_method", e_sp_method);
       	conf.setDouble("epsilon_sp", epsilon_sp);
       	conf.setInt("epsilon_t", epsilon_t);
       	conf.setInt("dt", dt);

  	
       	conf.set("fs.default.name", hostname.concat(dfs_port));
       	
       	conf.setBoolean("dfs.support.append", true);
       	conf.set("mapreduce.task.io.sort.factor", "100");
    	conf.set("mapreduce.task.io.sort.mb", "64");
    	conf.set("mapreduce.map.sort.spill.percent", "0.9");
       	conf.set("mapreduce.job.reduce.slowstart.completedmaps", "1.00");

    	conf.set("mapreduce.jobtracker.address", "local");
       	conf.set("mapreduce.framework.name", "yarn");
       	conf.set("yarn.resourcemanager.address", hostname.concat(rm_port));
       	
       	/*
       	conf.set("mapreduce.map.java.opts", "-Xmx720m");//75%
       	conf.set("mapreduce.reduce.java.opts", "-Xmx720m");//75%
       	conf.set("mapred.child.java.opts", "-Xmx960m");
       	conf.set("mapreduce.map.memory.mb", "960");
       	conf.set("mapreduce.reduce.memory.mb", "960");
       	*/
       	
       	conf.set("mapreduce.map.output.compress", "true");
        conf.set("mapreduce.map.output.compress.codec", "org.apache.hadoop.io.compress.Lz4Codec");
        
    	conf.set("mapreduce.reduce.shuffle.input.buffer.percent", "0.2");
        
    	if(fs.exists(new Path(workspace_dir.concat(output_dir)))){
    		fs.delete(new Path(workspace_dir.concat(output_dir)), true);
    	}


		Job job = Job.getInstance(conf, job_description);
		
		job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);//avoid running multiple attempts of the same task
		
		job.setNumReduceTasks(nof_reducers);
		
		job.setJarByClass(DSJDriver.class);
		job.setPartitionerClass(DSJPartitioner.class);
		job.setGroupingComparatorClass(DSJGroupingComparator.class);
		job.setMapperClass(DSJMapper.class);

		job.setReducerClass(DSJReducer.class);


		job.setInputFormatClass(MyBloatFileInputFormat.class);

		job.setOutputKeyClass(DTJrPointElement.class);
		job.setOutputValueClass(DTJrPointElement.class);
		job.setMapOutputKeyClass(DTJrPairElement.class);
		job.setMapOutputValueClass(Text.class);
		
		job.addCacheFile(new URI(hostname.concat(dfs_port).concat(workspace_dir).concat("/octtree")));
		job.addCacheFile(new URI(hostname.concat(dfs_port).concat(workspace_dir).concat("/TotalOrderPartitioner")));

		MyBloatFileInputFormat.setInputPaths(job, new Path(hostname.concat(dfs_port).concat(workspace_dir).concat(input_dir)));

        LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);

		FileOutputFormat.setOutputPath(job, new Path(hostname.concat(dfs_port).concat(workspace_dir).concat(output_dir)));

		job.waitForCompletion(true);

    }
}

