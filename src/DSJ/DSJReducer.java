package DSJ;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import CustomWritables.DTJrPairElement;
import CustomWritables.DTJrPointElement;

import DataTypes.Period;
import DataTypes.PointSP;
import DataTypes.PointST;


public class DSJReducer extends Reducer<DTJrPairElement, Text, DTJrPointElement, List<DTJrPointElement>> {

	
    String workspace_dir = new String();
    String dtjmr_subtraj_dir = new String();
    int epsilon_t;
    int dt;

	List<Integer> borders = new ArrayList<Integer>();


    FSDataOutputStream fileOutputStream;
    PrintWriter writer;

	@Override
	public void setup(Context context) throws IOException {
		
	    Configuration conf = context.getConfiguration();
	    
        workspace_dir = conf.get("workspace_dir");
        epsilon_t = Integer.parseInt(conf.get("epsilon_t"));
        dt = Integer.parseInt(conf.get("dt"));
	}

	HashMap<Integer,Period> RightTrajDuration = new HashMap<Integer,Period>();
	Period match_duration = new Period();

	DTJrPointElement output_key = new DTJrPointElement();
	List<DTJrPointElement> output_value = new ArrayList<DTJrPointElement>();

	PointST point = new PointST();
	
	DTJrPointElement Element = new DTJrPointElement();
	
	ArrayList<DTJrPointElement> L = new ArrayList<DTJrPointElement>();
	HashSet<Integer> F = new HashSet<Integer>();
	
	
	TreeMap<Integer,List<DTJrPointElement>> MatchList = new TreeMap<Integer,List<DTJrPointElement>>();
	HashMap<Integer,HashSet<Integer>> FalseAfter = new HashMap<Integer,HashSet<Integer>>();
	HashMap<Integer,HashSet<Integer>> FalseBefore = new HashMap<Integer,HashSet<Integer>>();
	
	HashMap<Integer,DTJrPointElement> key_point = new HashMap<Integer,DTJrPointElement>();

	LinkedHashMap<Integer, HashSet<DTJrPointElement>> Result = new LinkedHashMap<Integer, HashSet<DTJrPointElement>>();
	Map<Integer, List<DTJrPointElement>> tmp_result = new HashMap<Integer, List<DTJrPointElement>>();
	List<DTJrPointElement>  intersection = new ArrayList<DTJrPointElement>();
	

	boolean match;
	boolean after;
	String match_after;
	
	int prev_obj_id = 0;
	int prev_traj_id = 0;
	PointST prev_point = new PointST();
	int prev_dt = Integer.MAX_VALUE;
	double prev_dist_sp = Integer.MAX_VALUE;;
	String line = new String();
	StringTokenizer linetokenizer;
	StringTokenizer linetokenizer2;

	

	public void reduce(DTJrPairElement _key, Iterable<Text> values, Context context)
			throws IOException, InterruptedException {
		
		
		// process values
		MatchList.clear();
		FalseAfter.clear();
		FalseBefore.clear();
		Result.clear();
		key_point.clear();
		
	for (Text val : values) {
			
			Result.put(_key.r_p.t, new HashSet<DTJrPointElement>());
			line = val.toString();
    		
	        linetokenizer = new StringTokenizer(line, ",");
	        
	        while (linetokenizer.hasMoreTokens()) {
	        	
	        	match = Boolean.parseBoolean(linetokenizer.nextToken());
	        	after = Boolean.parseBoolean(linetokenizer.nextToken());
	        	
	        }

			key_point.put(_key.r_p.t, new DTJrPointElement(_key.r_obj_id, _key.r_traj_id, _key.r_p));

			DTJrPointElement Element = new DTJrPointElement(_key.s_obj_id, _key.s_traj_id, _key.s_p);
	        
			if (match == true){
				
				if (!MatchList.containsKey(_key.r_p.t)){
					
					if (!MatchList.isEmpty() && MatchList.lastKey() - MatchList.firstKey() >= dt + 2*epsilon_t){
						
						int l = MatchList.firstKey();
						int first_key;

						tmp_result.clear();
						tmp_result.put(l, MatchList.get(l));
						int prev_p_key = l;
						int next_l = 0;
						int counter = 0;


						for (Map.Entry<Integer,List<DTJrPointElement>> entry : MatchList.entrySet()){

							if (counter == 1){
								next_l = entry.getKey();
							}

							intersection.clear();
							
							int m = 0;
							int n = 0;

							//List intersection in O(n)
							while (m < tmp_result.get(prev_p_key).size() && n < entry.getValue().size()){

								if (tmp_result.get(prev_p_key).get(m).obj_id > entry.getValue().get(n).obj_id){
									
									n++;
									
								} else if (tmp_result.get(prev_p_key).get(m).obj_id < entry.getValue().get(n).obj_id){
									
									m++;
									
								} else if (tmp_result.get(prev_p_key).get(m).obj_id == entry.getValue().get(n).obj_id){
									
									if (tmp_result.get(prev_p_key).get(m).traj_id > entry.getValue().get(n).traj_id){
										
										n++;
										
									} else if (tmp_result.get(prev_p_key).get(m).traj_id < entry.getValue().get(n).traj_id){
										
										m++;
										
									} else if (tmp_result.get(prev_p_key).get(m).traj_id == entry.getValue().get(n).traj_id){
										
										if (entry.getValue().get(n).obj_id != 0 && entry.getValue().get(n).traj_id !=0){

											if ((!FalseAfter.isEmpty() && FalseAfter.containsKey(prev_p_key) && entry.getKey() != l  && FalseAfter.get(prev_p_key).contains(entry.getValue().get(n).obj_id)) || (!FalseBefore.isEmpty() && FalseBefore.containsKey(entry.getKey()) && entry.getKey() != l && FalseBefore.get(entry.getKey()).contains(entry.getValue().get(n).obj_id))){
												
												//Do Nothing
			
											} else {
												
												if (!RightTrajDuration.containsKey(entry.getValue().get(n).obj_id)){
												
													RightTrajDuration.put(entry.getValue().get(n).obj_id, new Period(Math.max(entry.getValue().get(n).point.t, entry.getKey()), Math.min(entry.getValue().get(n).point.t, entry.getKey())));
												
												} else {

													if (Math.max(entry.getValue().get(n).point.t, entry.getKey()) < RightTrajDuration.get(entry.getValue().get(n).obj_id).ti){

														RightTrajDuration.get(entry.getValue().get(n).obj_id).setPeriod_ti(Math.max(entry.getValue().get(n).point.t, entry.getKey()));
													
													} 
													if (Math.min(entry.getValue().get(n).point.t, entry.getKey()) > RightTrajDuration.get(entry.getValue().get(n).obj_id).te){
	
														RightTrajDuration.get(entry.getValue().get(n).obj_id).setPeriod_te(Math.min(entry.getValue().get(n).point.t, entry.getKey()));
	
														
													}
												
											}
	
												intersection.add(entry.getValue().get(n));
												
											}

										}

										m++;
										n++;
										
									}

								}

							}

							tmp_result.put(entry.getKey(), new ArrayList<DTJrPointElement>(intersection));

							prev_p_key = entry.getKey();
							counter ++;

						}
								
						first_key = l;
						
						for (Map.Entry<Integer, List<DTJrPointElement>> p : MatchList.entrySet()){

							if (p.getKey() - first_key >= dt){
								
								int counter2 = 0;
								int next_first_key = 0;
								
								for (Map.Entry<Integer, List<DTJrPointElement>> pp : MatchList.subMap(first_key, true, p.getKey(), true).entrySet()){

									if (counter2 == 1){
										next_first_key = p.getKey();
									}
									
									intersection.clear();
									
									int m = 0;
									int n = 0;
									
									while (m < tmp_result.get(p.getKey()).size() && n < tmp_result.get(pp.getKey()).size()){
										

										if (tmp_result.get(p.getKey()).get(m).obj_id > tmp_result.get(pp.getKey()).get(n).obj_id){
											
											n++;
											
										} else if (tmp_result.get(p.getKey()).get(m).obj_id < tmp_result.get(pp.getKey()).get(n).obj_id){
											
											m++;
											
										} else if (tmp_result.get(p.getKey()).get(m).obj_id == tmp_result.get(pp.getKey()).get(n).obj_id){
											
											if (tmp_result.get(p.getKey()).get(m).traj_id > tmp_result.get(pp.getKey()).get(n).traj_id){
												
												n++;
												
											} else if (tmp_result.get(p.getKey()).get(m).traj_id < tmp_result.get(pp.getKey()).get(n).traj_id){
												
												m++;
												
											} else if (tmp_result.get(p.getKey()).get(m).traj_id == tmp_result.get(pp.getKey()).get(n).traj_id){

												if (RightTrajDuration.get(tmp_result.get(pp.getKey()).get(n).obj_id).Duration() >= dt){

													intersection.add(tmp_result.get(pp.getKey()).get(n));
											
												}

												m++;
												n++;
												
											}
										}

									}
									
									if (!Result.containsKey(pp.getKey())){

										Result.put(pp.getKey(), new HashSet<DTJrPointElement>(intersection));
										
									} else {
										
										for (int j = 0; j < intersection.size(); j++){
											
											if (!Result.get(pp.getKey()).contains(intersection.get(j))){
												
												Result.get(pp.getKey()).add(intersection.get(j));
											} 							
										}
									}
									counter2++;
								}
								
								first_key = next_first_key;
							}

						}

						l = next_l;
					

						FalseAfter.remove(MatchList.firstKey());
						FalseBefore.remove(MatchList.firstKey());
						MatchList.remove(MatchList.firstKey());
					
					}
					
					ArrayList<DTJrPointElement> L = new ArrayList<DTJrPointElement>();
					
					L.add(Element);
					MatchList.put(_key.r_p.t, L);
					prev_dt = Math.abs(Element.point.t - _key.r_p.t);
					prev_dist_sp = Element.point.p.DistanceSP(_key.r_p.p);

				} else {
					
					if (Element.obj_id == prev_obj_id && Element.traj_id == prev_traj_id){
						
						if (Element.point.p.DistanceSP(_key.r_p.p) < prev_dist_sp){

							prev_dt = Math.abs(Element.point.t - _key.r_p.t);
							prev_dist_sp = Element.point.p.DistanceSP(_key.r_p.p);

							MatchList.get(_key.r_p.t).remove(MatchList.get(_key.r_p.t).size()-1);
							MatchList.get(_key.r_p.t).add(Element);
							
						}
						
					} else {
						
						 MatchList.get(_key.r_p.t).add(Element);
						 prev_dt = Math.abs(Element.point.t - _key.r_p.t);
						 prev_dist_sp = Element.point.p.DistanceSP(_key.r_p.p);

					}
					
				}
				
				prev_obj_id = Element.obj_id;
				prev_traj_id = Element.traj_id;
				prev_point = new PointST(Element.point);

			} else{
				
				if (after == true){
					
					if (!FalseAfter.containsKey(_key.r_p.t)){
						
						HashSet<Integer> F = new HashSet<Integer>();
						
						F.add(Element.obj_id);
						FalseAfter.put(_key.r_p.t, F);

					} else {
			
						FalseAfter.get(_key.r_p.t).add(Element.obj_id);

					}
					
				} else {
					
					if (!FalseBefore.containsKey(_key.r_p.t)){
						
						HashSet<Integer> F = new HashSet<Integer>();
				
						F.add(Element.obj_id);
						FalseBefore.put(_key.r_p.t, F);

					} else {
			
						FalseBefore.get(_key.r_p.t).add(Element.obj_id);

					}
	
				}


			}
		}
		
		if (!MatchList.isEmpty()){
			
			int l = MatchList.firstKey();
			int first_key;
			
			RightTrajDuration.clear();

			tmp_result.clear();
			tmp_result.put(l, MatchList.get(l));
			int prev_p_key = l;
			int next_l = 0;
			int counter = 0;


			for (Map.Entry<Integer,List<DTJrPointElement>> entry : MatchList.entrySet()){

				if (counter == 1){
					next_l = entry.getKey();
				}

				intersection.clear();
				
				int m = 0;
				int n = 0;

				//List intersection in O(n)
				while (m < tmp_result.get(prev_p_key).size() && n < entry.getValue().size()){

					if (tmp_result.get(prev_p_key).get(m).obj_id > entry.getValue().get(n).obj_id){
						
						n++;
						
					} else if (tmp_result.get(prev_p_key).get(m).obj_id < entry.getValue().get(n).obj_id){
						
						m++;
						
					} else if (tmp_result.get(prev_p_key).get(m).obj_id == entry.getValue().get(n).obj_id){
						
						if (tmp_result.get(prev_p_key).get(m).traj_id > entry.getValue().get(n).traj_id){
							
							n++;
							
						} else if (tmp_result.get(prev_p_key).get(m).traj_id < entry.getValue().get(n).traj_id){
							
							m++;
							
						} else if (tmp_result.get(prev_p_key).get(m).traj_id == entry.getValue().get(n).traj_id){
							
							if (entry.getValue().get(n).obj_id != 0 && entry.getValue().get(n).traj_id !=0){

								if ((!FalseAfter.isEmpty() && FalseAfter.containsKey(prev_p_key) && entry.getKey() != l  && FalseAfter.get(prev_p_key).contains(entry.getValue().get(n).obj_id)) || (!FalseBefore.isEmpty() && FalseBefore.containsKey(entry.getKey()) && entry.getKey() != l && FalseBefore.get(entry.getKey()).contains(entry.getValue().get(n).obj_id))){
									
									//Do Nothing

								} else {
									
									if (!RightTrajDuration.containsKey(entry.getValue().get(n).obj_id)){
									
										RightTrajDuration.put(entry.getValue().get(n).obj_id, new Period(Math.max(entry.getValue().get(n).point.t, entry.getKey()), Math.min(entry.getValue().get(n).point.t, entry.getKey())));
									
									} else {
	
										if (Math.max(entry.getValue().get(n).point.t, entry.getKey()) < RightTrajDuration.get(entry.getValue().get(n).obj_id).ti){
	
											RightTrajDuration.get(entry.getValue().get(n).obj_id).setPeriod_ti(Math.max(entry.getValue().get(n).point.t, entry.getKey()));
											
										} 
										if (Math.min(entry.getValue().get(n).point.t, entry.getKey()) > RightTrajDuration.get(entry.getValue().get(n).obj_id).te){
	
											RightTrajDuration.get(entry.getValue().get(n).obj_id).setPeriod_te(Math.min(entry.getValue().get(n).point.t, entry.getKey()));
	
											
										}
										
									}

									intersection.add(entry.getValue().get(n));
									
								}

							}

							m++;
							n++;
							
						}

					}

				}

				tmp_result.put(entry.getKey(), new ArrayList<DTJrPointElement>(intersection));

				prev_p_key = entry.getKey();
				counter ++;

			}
					
			first_key = l;
			
			for (Map.Entry<Integer, List<DTJrPointElement>> p : MatchList.entrySet()){

				if (p.getKey() - first_key >= dt){
					
					int counter2 = 0;
					int next_first_key = 0;
					
					for (Map.Entry<Integer, List<DTJrPointElement>> pp : MatchList.subMap(first_key, true, p.getKey(), true).entrySet()){

						if (counter2 == 1){
							next_first_key = p.getKey();
						}
						
						intersection.clear();
						
						int m = 0;
						int n = 0;
						
						while (m < tmp_result.get(p.getKey()).size() && n < tmp_result.get(pp.getKey()).size()){
							

							if (tmp_result.get(p.getKey()).get(m).obj_id > tmp_result.get(pp.getKey()).get(n).obj_id){
								
								n++;
								
							} else if (tmp_result.get(p.getKey()).get(m).obj_id < tmp_result.get(pp.getKey()).get(n).obj_id){
								
								m++;
								
							} else if (tmp_result.get(p.getKey()).get(m).obj_id == tmp_result.get(pp.getKey()).get(n).obj_id){
								
								if (tmp_result.get(p.getKey()).get(m).traj_id > tmp_result.get(pp.getKey()).get(n).traj_id){
									
									n++;
									
								} else if (tmp_result.get(p.getKey()).get(m).traj_id < tmp_result.get(pp.getKey()).get(n).traj_id){
									
									m++;
									
								} else if (tmp_result.get(p.getKey()).get(m).traj_id == tmp_result.get(pp.getKey()).get(n).traj_id){
	
									if (RightTrajDuration.get(tmp_result.get(pp.getKey()).get(n).obj_id).Duration() >= dt){

										intersection.add(tmp_result.get(pp.getKey()).get(n));
								
									}
									m++;
									n++;
									
								}
							}

						}
						
						if (!Result.containsKey(pp.getKey())){

							Result.put(pp.getKey(), new HashSet<DTJrPointElement>(intersection));
							
									
						} else {
							
							for (int j = 0; j < intersection.size(); j++){
								
								if (!Result.get(pp.getKey()).contains(intersection.get(j))){
									
									Result.get(pp.getKey()).add(intersection.get(j));
								} 							
							}
						}
						
						counter2++;
					}
					
					first_key = next_first_key;
				}

			}

			l = next_l;
		
			FalseAfter.remove(MatchList.firstKey());
			FalseBefore.remove(MatchList.firstKey());
			MatchList.remove(MatchList.firstKey());

		}

		for (Map.Entry<Integer,HashSet<DTJrPointElement>> entry_hr : Result.entrySet()){
				
			PointSP key_p = new PointSP(key_point.get(entry_hr.getKey()).point.p);
			output_key = new DTJrPointElement(_key.r_obj_id, _key.r_traj_id, new PointST(entry_hr.getKey(), key_p));
			
			Iterator<DTJrPointElement> it = entry_hr.getValue().iterator();
		    
			while(it.hasNext()){
		    
				output_value.clear();
				
			    output_value.add(it.next());

				context.write(output_key, output_value);
		    
			}
			

		}
	}

}
