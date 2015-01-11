package com.gy.DAO;
 
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;  
import com.gy.CarMonitor.*;  
import com.gy.Entity.OffLineStatEntity;
public class OffLineDAOold {
	//�ն���������ϸ��
	class OffLineDetails {
		public String ter_id,on_off_flag,cp_name;
		public Integer offduration;
		public Long create_time ; 
	}
	String id = null; 
	Statement stat = null;
	ResultSet rs = null; 
	Connection conn=null;  
	/**
	 * ��ȡ����ͳ����Ϣ 
	 * 
	 * */
	public List<OffLineStatEntity> getOffLineStat(int querydays){
		List<OffLineDetails> listofflinedetails = new ArrayList<OffLineDetails>();
		/*
		 *select ter2.plate_no || '-' || t.ter_id as ter_id,
		       ter2.cp_name,
		       t.on_off_flag,
		       t.create_time,
		       gintime(trunc(sysdate - 0)) as firstime,
		       create_time - lastcrtime
		  from (select t.*,
		               lag(create_time, 1, gintime(trunc(sysdate))) over(partition by ter_id order by id asc) as lastcrtime
		          from sa.tbl_s_terminal_online t
		         where trunc(gtime(create_time)) = trunc(sysdate) 
		        ) t
		  left join (select ter.id, ter.plate_no, com.cp_name
		               from sa.tbl_s_terminal ter, sa.tbl_company com
		              where ter.cp_id = com.id) ter2
		    on t.ter_id = ter2.id; 
		 * */
		String sql= "select " +
				"ter2.plate_no||'-'||t.ter_id as ter_id ," +
				"ter2.cp_name," + 				
				"t.on_off_flag," +
				"t.create_time, " +
				"gintime(trunc(sysdate - "+ querydays+ 
				")) as firstime," + 
				"trunc(nvl(t.create_time-t2.create_time,t.create_time-gintime(trunc(sysdate) -" +
				querydays +
				"))) as 	 " +
				" from " +
				"(   select ter_id,on_off_flag,create_time,rownum as rownum1  from " +
				"(select distinct  ter_id, on_off_flag,create_time,at_time  " +
				"from tbl_s_terminal_online where " +
				"trunc(gtime(create_time)) = trunc(sysdate) - " +
				querydays+
				" order by ter_id,  create_time asc ,at_time asc  ))T " +
				"LEFT join " +
				"(   select ter_id,on_off_flag,create_time,rownum+1 as rownum1  " +
				"from ( select distinct  ter_id, on_off_flag,create_time,at_time  " +
				"from tbl_s_terminal_online where trunc(gtime(create_time)) = trunc(sysdate) -" +
				querydays+
				"order by ter_id,  create_time asc ,at_time asc  ))T2 " +
				"on t.ter_id = t2.ter_id and t.rownum1 = t2.rownum1 "+
				" left join  (" + 
				" select ter.id,ter.plate_no,com.cp_name from sa.tbl_s_terminal ter ,sa.tbl_company  com where ter.cp_id= com.id ) ter2 "+ 
						   " on t.ter_id = ter2.id"  
//				" order by ter2.cp_name, t.ter_id asc"
				+ " where t.ter_id in(36) " ;
				;
		conn = new DBHelper().getConn(); 
		try{
			stat = conn.createStatement();
			System.err.println(sql);
			rs = stat.executeQuery(sql);
			while (rs.next()) {  
				OffLineDetails offlinetmp = new OffLineDetails();
				offlinetmp.ter_id = rs.getString("ter_id");
				offlinetmp.on_off_flag = rs.getString("on_off_flag");
				offlinetmp.cp_name = rs.getString("cp_name"); 
				try { 
					offlinetmp.offduration = Integer.valueOf( rs.getString("offduration"));
					offlinetmp.create_time = Long.parseLong(rs.getString("create_time"));
				} catch (Exception e) {
					System.err.println("�쳣��¼"+rs.getString("ter_id"));	
					e.printStackTrace();
				}
				listofflinedetails.add( offlinetmp);
			}
		}
		catch(SQLException ex){
			ex.printStackTrace(); 
		}
		//�ر����ݿ�
		closeDB(); 
		//����ͳ�ƺ�����߽��
		return  getOffLineStat(listofflinedetails,querydays);
//		return null;
	}
	
	//���ش����������ͳ�ƽ��
		public   List<OffLineStatEntity> getOffLineStat(List<OffLineDetails>  listofflinedetails,int querydays){
			 List<OffLineStatEntity> listofflinestat = new ArrayList<OffLineStatEntity>();
			 String terid_tmp ="initid";	//�ն�ID 
			 int  offcount =0;	   // ���ߴ���
			 long offseconds = 0;  //����ʱ����
			 long onSeconds=0; //����ʱ��
			 int  recordNUM= 0 ;    //��¼��   
			 for (int i = 0; i < listofflinedetails.size(); i++) {
				 OffLineDetails  offdetail = listofflinedetails.get(i);
				//�ж��ն��Ƿ���� ��ʼ���ն�ID
				 if (i==0) {
					 terid_tmp = offdetail.ter_id; 
				} 
//				 if (offdetail.ter_id.equals("��A5C711-30")) {
//					System.err.println("��A5C711-30");
//				}
				 //�����µ��ն� �Ҳ��ǵ�һ����¼
				 if (!offdetail.ter_id.equals(terid_tmp)& i>0) {
							 OffLineStatEntity offlinestat = new OffLineStatEntity();
							 offlinestat.plate_no = terid_tmp;
							 offlinestat.offcount = String.valueOf( offcount);		
							 offlinestat.cp_name =  listofflinedetails.get(i-1).cp_name;
							 //״̬Ϊ1�������ߣ�˵��ǰ��һ��ʱ���Ϊ����ʱ��	 
								if( listofflinedetails.get(i-1).on_off_flag.equals("1")){ 			
									//����ʱ�� ���һ��������״̬ �����ս�����ʱ��
									onSeconds=onSeconds+getlastofftimes(  listofflinedetails.get(i-1).create_time,querydays);
								}else {
									//���һ������ʱ��
									offseconds=offseconds+getlastofftimes(listofflinedetails.get(i-1).create_time,querydays);						 
								}					 
							 //�������ֻ��һ����¼���
							 if (offseconds==0 & onSeconds>0& querydays>0) {
								 offseconds=86400 -onSeconds;
							 }else if (offseconds>0 & onSeconds==0& querydays>0) {
								 onSeconds=86400 -offseconds;
							 }
							 offlinestat.offSecends= String.valueOf(offseconds);
							 offlinestat.onSeconds =String.valueOf(onSeconds);  //��������ʱ��
							 
							 listofflinestat.add(offlinestat);
							 
							 //����ͳ�Ʋ���
							   offcount =0;		    // ���ߴ���
							   offseconds = 0;  	//����ʱ����
							   onSeconds=0;    //����ʱ��
							   recordNUM= 0 ;		//��¼��  
							   terid_tmp =  offdetail.ter_id;
				} 
				if( offdetail.on_off_flag.equals("1")){  //״̬Ϊ1�������ߣ�˵��ǰ��һ��ʱ���Ϊ����ʱ��				
					offseconds=offseconds+offdetail.offduration;
					offcount++;
				}else {//����ʱ�� 
					onSeconds=onSeconds+offdetail.offduration;
				} 
				recordNUM++; 			
			 
	
					 //���һ���������
					 if (i==listofflinedetails.size()-1 ) {
							 OffLineStatEntity offlinestat = new OffLineStatEntity();
							 offlinestat.plate_no = terid_tmp; 
							 offlinestat.offcount = String.valueOf( offcount);	
							 offlinestat.cp_name= offdetail.cp_name;
							 //״̬Ϊ1�������ߣ�˵��ǰ��һ��ʱ���Ϊ����ʱ��	 
								if( offdetail.on_off_flag.equals("1")){ 			
									//����ʱ��
									onSeconds=onSeconds+getlastofftimes(offdetail.create_time,querydays);
								}else {
									//���һ������ʱ��
									offseconds=offseconds+getlastofftimes(offdetail.create_time,querydays);						 
								}
							offlinestat.offSecends= String.valueOf(offseconds);
							offlinestat.onSeconds = String.valueOf(onSeconds); ;  //��������ʱ��	
							listofflinestat.add(offlinestat);
					}			
			}			 			 
			 return sortlist(listofflinestat);
		}
		/**
		 * ���� 
		 * */
	private List<OffLineStatEntity>  sortlist(List<OffLineStatEntity>  list){
		OffLineStatEntity[] offarray = list.toArray(new OffLineStatEntity[list.size()] );
		for (int i = 0; i < offarray.length; i++) {
			for (int j = 0; j < offarray.length; j++) {
				if (((OffLineStatEntity)offarray[i]).cp_name.compareTo(offarray[j].cp_name)>=0) {
					OffLineStatEntity tmp =  offarray[i];
					offarray[i]=offarray[j];
					offarray[j]=tmp;
				}
			}
		}
//		System.err.println("===="+Arrays.asList(offarray));
//		for (int i = 0; i < offarray.length; i++) {
////			System.err.println("===="+offarray[i].cp_name+offarray[i].plate_no);		
//		}
		return  Arrays.asList(offarray );
		
	} 
	private void closeDB(){
		//�ر����ݿ�
		try {
			System.err.println("�ر����ݿ�����");
			if (rs !=null) {
				System.err.println("�ر�rs");
				rs.close();
			}
			if (stat!=null) {
				System.err.println("�ر�stat");
				stat.close();
			}
			if (conn!=null) {
				System.err.println("�ر�conn");
				conn.close();
			} 		
		} catch (SQLException e) { 
			e.printStackTrace();
		}
	}
	/**
	 * ��ȡ�ն�������ߺ���ͳ�Ƶ��յ�ʱ�� ������ 
	 * ��23ʱ ���� �򷵻�3600 
	 * curtimeͳ��������ʱ�� queryday��ѯʱ�� offsends����ʱ����
	 **/
	public static long getlastofftimes(long curtime,int queryday ){
		if (queryday==0) {
			return System.currentTimeMillis()/1000-curtime ;
		}
		else {			
			Date d=new Date();
			SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd"); 
			String d2 = df.format(new Date(d.getTime() - queryday * 24 * 60 * 60 * 1000));
			try {
				Date date = df.parse(d2); //�õ��賿��ʱ�� 
				return  (86400+date.getTime()/1000 -curtime);
			} catch (ParseException e) { 
				e.printStackTrace();
			}	
		}
		return 0;
	} 
	private static String getTimes(Long longtime){
		return longtime/3600+"ʱ"+ (longtime%3600)/60+"��"+ longtime%60+"��";
	}
	
	/**
	 * 
	 * */
	public static void main(String[] args) {
		OffLineDAOold off = new OffLineDAOold();  
		List<OffLineStatEntity> listofflinestat = off.getOffLineStat(0);
//		System.err.println(listofflinestat.size());
		for (int i = 0; i < listofflinestat.size(); i++) { 
			System.err.println("�ն�:"+listofflinestat.get(i).cp_name+listofflinestat.get(i).plate_no  +
					" ������ʱ��Ϊ"+getTimes(Long.valueOf(listofflinestat.get(i).offSecends))+ 
			"����ʱ��"+getTimes(Long.valueOf(listofflinestat.get(i).onSeconds)) + "     ���ߴ���" +	listofflinestat.get(i).offcount 
			);
		} 
 
		 
	}
}
