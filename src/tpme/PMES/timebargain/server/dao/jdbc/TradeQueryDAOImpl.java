package tpme.PMES.timebargain.server.dao.jdbc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tpme.PMES.timebargain.server.dao.SupperDAO;
import tpme.PMES.timebargain.server.dao.TradeQueryDAO;
import tpme.PMES.timebargain.server.model.DelayFee_RT;
import tpme.PMES.timebargain.server.model.DelayFee_Step_RT;
import tpme.PMES.timebargain.server.model.Fee_RT;
import tpme.PMES.timebargain.server.model.HoldQty_RT;
import tpme.PMES.timebargain.server.model.Margin_RT;
import tpme.PMES.timebargain.server.model.OrderPoint_RT;
import tpme.PMES.timebargain.server.model.Privilege;
import tpme.PMES.timebargain.server.model.QuotePoint_RT;
import tpme.PMES.timebargain.server.model.TradeAuth_RT;


/**
 * 将查询权限独立出来
 * @author suzh
 *
 */
public class TradeQueryDAOImpl extends SupperDAO implements TradeQueryDAO {
	private final Log log = LogFactory.getLog(TradeQueryDAOImpl.class);

	/**
	 * 根据登陆的交易员ID获取交易员权限和交易商权限 初始化Privilege对象
	 */
	public Privilege getTradePrivilege(Privilege privilege) {
		StringBuffer sb = new StringBuffer();
		if ("0".equals(privilege.getFirmType())) {
			sb.append("select m_firmid memberno from t_customer where firmid = ?");
		} else {
			sb.append("select sm_firmid memberno from t_memberrelation where m_Firmid = ? order by sortno");
		}

		Object[] params = null;
		params = new Object[]{privilege.getFirmId()};

		log.debug("sql: " + sb.toString());
		if (params != null) {
			for (int i = 0, len = params.length; i < len; i++) {
				log.debug("params[" + i + "]: " + params[i]);
			}
		}

		List list = getJdbcTemplate().queryForList(sb.toString(), params);

		if (null != list && list.size() > 0) {
			Map map = (Map) list.get(0);
			privilege.setM_FirmID((String) map.get("memberno"));
//			privilege.setM_FirmName((String) map.get("Name"));
		} else {
			privilege.setM_FirmID("");
			privilege.setM_FirmName("");
		}
		
		String statusSql = "";
		if ("0".equals(privilege.getFirmType())) {
			statusSql = "select a.status from t_customer a where a.firmid=? ";
		} else if("1".equals(privilege.getFirmType())){
			statusSql = "select a.status,a.membertype from t_compmember a " 
					+ "where a.m_firmId=? ";
		} else {
			statusSql = "select a.status from t_specialmember a " 
					+ "where a.m_firmId=? ";
		}

		Map statusMap = getJdbcTemplate().queryForMap(statusSql,
				new Object[]{privilege.getFirmId()});

		privilege.setStatus((String) statusMap.get("status"));

		// 如果会员则设置会员类型
		if ("1".equals(privilege.getFirmType())) {
			privilege.setMemberType((String) statusMap.get("membertype"));
		}

		// 如果是 终止状态 或者客户冻结状态禁止登录 直接返回
		if ("D".equals(privilege.getStatus())
				|| ("F".equals(privilege.getStatus()) 
						&& "0".equals(privilege.getFirmType()))) {
			return privilege;
		}

		// 设置交易商特殊交易参数
		String rt_sql ;
		String auth_sql = "";
		List<Map> authRt_List = null;
		Object[] auth_params = {};
		if("2".equals(privilege.getFirmType())){
			rt_sql = "select m.CommodityID, m.MarginAlgr, " 
					+ "m.TradeMargin, m.SettleMargin, m.HolidayMargin, "
					+ "f.FeeAlgr, decode(f.FeeAlgr, 1, TRUNC(nvl(f.FeeRate,0),3), 2, nvl(f.FeeRate,0)) ccpFeeRate, decode(f.FeeAlgr, 1, TRUNC(nvl(f.FeeRate,0),3), 2, nvl(f.FeeRate,0)) FeeRate, f.FeeMode, "
					+ "t.Display, t.Cancel_L_Open, t.Cancel_StopLoss, " 
					+ "t.Cancel_StopProfit, t.M_B_Open, t.M_B_Close, "
					+ "t.L_B_Open, t.B_StopLoss, t.B_StopProfit, t.M_S_Open, " 
					+ "t.M_S_Close, t.L_S_Open, t.S_StopLoss, t.S_StopProfit, "
					+ "h.OneMaxOrderQty, h.OneMinOrderQty, h.MaxNetHold, h.MaxHoldQty,t.c_l_b_open,t.c_l_b_close,t.c_l_s_open,t.c_l_s_close "
					+ "from T_C_Margin_RT m, T_C_Fee_RT f, T_C_TradeAuth_RT t,  T_C_HoldQty_RT h "
					+ "where m.FirmID = f.FirmID and m.CommodityID = f.CommodityID "
					+ "and m.FirmID = t.FirmID and m.CommodityID = t.CommodityID "
					+ "and m.FirmID = h.FirmID and m.CommodityID = h.CommodityID "
					+ "and m.FirmID = ?";
			params = new Object[]{privilege.getFirmId()};
		}else if("1".equals(privilege.getFirmType())) {
			rt_sql = "select m.CommodityID, m.MarginAlgr, " 
					+ "m.TradeMargin, m.SettleMargin, m.HolidayMargin, "
					+ "f.FeeAlgr, decode(f.FeeAlgr, 1, TRUNC(nvl(f.FeeRate,0),4), 2, nvl(f.FeeRate,0)) ccpFeeRate, decode(f.FeeAlgr, 1, TRUNC(nvl(f.FeeRate,0),4), 2, nvl(f.FeeRate,0)) FeeRate, f.FeeMode, "
					+ "t.Display, t.Cancel_L_Open, t.Cancel_StopLoss, " 
					+ "t.Cancel_StopProfit, t.M_B_Open, t.M_B_Close, "
					+ "t.L_B_Open, t.B_StopLoss, t.B_StopProfit, t.M_S_Open, " 
					+ "t.M_S_Close, t.L_S_Open, t.S_StopLoss, t.S_StopProfit, "
					+ "o.StopLossPoint, o.StopProfitPoint, o.L_Open_Point, " 
					+ "o.M_OrderPoint, o.Min_M_OrderPoint, o.Max_M_OrderPoint, "
					+ "h.OneMaxOrderQty, h.OneMinOrderQty, h.MaxNetHold, h.MaxHoldQty,t.c_l_b_open,t.c_l_b_close,t.c_l_s_open,t.c_l_s_close "
					+ "from T_C_Margin_RT m, T_C_Fee_RT f, T_C_TradeAuth_RT t, T_C_OrderPoint_RT o, T_C_HoldQty_RT h "
					+ "where m.FirmID = f.FirmID and m.CommodityID = f.CommodityID "
					+ "and m.FirmID = t.FirmID and m.CommodityID = t.CommodityID "
					+ "and m.FirmID = o.FirmID and m.CommodityID = o.CommodityID "
					+ "and m.FirmID = h.FirmID and m.CommodityID = h.CommodityID "
					+ "and m.FirmID = ?";
			params = new Object[]{privilege.getFirmId()};
		}else{
			rt_sql = "select m.CommodityID,"
					+"       m.MarginAlgr,"
					+"       m.TradeMargin,"
					+"       m.SettleMargin,"
					+"       m.HolidayMargin,"
					+"       t.feealgr FeeAlgr,"
					+"       3 FeeMode,"
					+"       decode(t.feealgr, 1, nvl(t.FeeRate, 0), 2, nvl(t.FeeRate, 0)) FeeRate,"
					+"       decode(t.feealgr, 1, nvl(t.CcpFeeRate, 0), 2, nvl(t.CcpFeeRate, 0)) CcpFeeRate,"
					+"       nvl(f.Display, 0) Display,"
					+"       nvl(f.Cancel_L_Open, 0) Cancel_L_Open,"
					+"       nvl(f.Cancel_StopLoss, 0) Cancel_StopLoss,"
					+"       nvl(f.Cancel_StopProfit, 0) Cancel_StopProfit,"
					+"       nvl(f.M_B_Open, 0) M_B_Open,"
					+"       nvl(f.M_B_Close, 0) M_B_Close,"
					+"       nvl(f.L_B_Open, 0) L_B_Open,"
					+"       nvl(f.B_StopLoss, 0) B_StopLoss,"
					+"       nvl(f.B_StopProfit, 0) B_StopProfit,"
					+"       nvl(f.M_S_Open, 0) M_S_Open,"
					+"       nvl(f.M_S_Close, 0) M_S_Close,"
					+"       nvl(f.L_S_Open, 0) L_S_Open,"
					+"       nvl(f.S_StopLoss, 0) S_StopLoss,"
					+"       nvl(f.S_StopProfit, 0) S_StopProfit,"
					+"       nvl(f.c_l_b_open, 0) c_l_b_open,"
					+"       nvl(f.c_l_b_close, 0) c_l_b_close,"
					+"       nvl(f.c_l_s_open, 0) c_l_s_open,"
					+"       nvl(f.c_l_s_close, 0) c_l_s_close,"
					+"       o.StopLossPoint,"
					+"       o.StopProfitPoint,"
					+"       o.L_Open_Point,"
					+"       o.M_OrderPoint,"
					+"       o.Min_M_OrderPoint,"
					+"       o.Max_M_OrderPoint,"
					+"       h.OneMaxOrderQty,"
					+"       h.OneMinOrderQty,"
					+"       h.MaxNetHold,"
					+"       h.MaxHoldQty"
					+"  from T_C_Margin_RT m,"
					+"       T_C_OrderPoint_RT o,"
					+"       T_C_HoldQty_RT h,"
					+"       t_commodity_rt r,"
					+"       (select mg.firmid,"
					+"               tcg.commodityid,"
					+"               decode(tcgs.entermarket,1,nvl(tcg.Display, 0),0) Display,"
					+"               nvl(tcg.Cancel_L_Open, 0) Cancel_L_Open,"
					+"               nvl(tcg.Cancel_StopLoss, 0) Cancel_StopLoss,"
					+"               nvl(tcg.Cancel_StopProfit, 0) Cancel_StopProfit,"
					+"               nvl(tcg.M_B_Open, 0) M_B_Open,"
					+"               nvl(tcg.M_B_Close, 0) M_B_Close,"
					+"               nvl(tcg.L_B_Open, 0) L_B_Open,"
					+"               nvl(tcg.B_StopLoss, 0) B_StopLoss,"
					+"               nvl(tcg.B_StopProfit, 0) B_StopProfit,"
					+"               nvl(tcg.M_S_Open, 0) M_S_Open,"
					+"               nvl(tcg.M_S_Close, 0) M_S_Close,"
					+"               nvl(tcg.L_S_Open, 0) L_S_Open,"
					+"               nvl(tcg.S_StopLoss, 0) S_StopLoss,"
					+"               nvl(tcg.S_StopProfit, 0) S_StopProfit,"
					+"               nvl(tcg.c_l_b_open, 0) c_l_b_open,"
					+"               nvl(tcg.c_l_b_close, 0) c_l_b_close,"
					+"               nvl(tcg.c_l_s_open, 0) c_l_s_open,"
					+"               nvl(tcg.c_l_s_close, 0) c_l_s_close"
					+"          from T_C_G_TRADEAUTH_RT tcg, M_GROUPFIRM mg ,t_c_g_submarketauth tcgs,t_comsubmarket tcsm"
					+"         where mg.firmid = ?"
					+"           and mg.groupid = tcg.groupid"
					+"           and tcgs.groupid = mg.groupid "
					+"           and tcgs.submarketid = tcsm.submarketid"
					+"           and tcg.commodityid = tcsm.commodityid) f,"
					+"       (select mg.firmid,"
					+"               aa.commodityid,"
					+"               aa.ccpFeeRate,"
					+"               aa.FeeRate,"
					+"               tr.feealgr"
					+"          from m_groupfirm mg,"
					+"               t_commodity_rt tr,"
					+"               (select tcf.groupid,"
					+"                       tcf.commodityid,"
					+"                       sum(decode(tcf.tradecategory, 1, tcf.feerate, 2, 0)) ccpFeeRate,"
					+"                       sum(decode(tcf.tradecategory, 1, 0, 2, tcf.feerate)) FeeRate"
					+"                  from t_c_g_fee_rt tcf"
					+"                 where tcf.bs_flag = 1"
					+"                   and tcf.tradecategory in (1, 2)"
					+"                 group by tcf.groupid, tcf.commodityid) aa"
					+"         where mg.groupid = aa.groupid(+)"
					+"           and aa.commodityid = tr.commodityid"
					+"           and tr.status = 1"
					+"           and mg.firmid = ?) t"
					+" where m.commodityid = r.commodityid"
					+"   and r.status = 1"
					+"   and m.FirmID = f.firmid(+)"
					+"   and m.CommodityID = f.CommodityID(+)"
					+"   and m.FirmID = t.firmid(+)"
					+"   and m.CommodityID = t.CommodityID(+)"
					+"   and m.FirmID = o.FirmID"
					+"   and m.CommodityID = o.CommodityID"
					+"   and m.FirmID = h.FirmID"
					+"   and m.CommodityID = h.CommodityID"
					+"   and m.firmid = ?";
			params = new Object[]{privilege.getFirmId(), privilege.getFirmId(), privilege.getFirmId()};
			
			auth_sql = "select display,"
					+"       cancel_l_open,"
					+"       cancel_stoploss,"
					+"       cancel_stopprofit,"
					+"       m_b_open,"
					+"       m_b_close,"
					+"       l_b_open,"
					+"       b_stoploss,"
					+"       b_stopprofit,"
					+"       m_s_open,"
					+"       m_s_close,"
					+"       l_s_open,"
					+"       s_stoploss,"
					+"       s_stopprofit,"
					+"       c_l_b_open,"
					+"       c_l_b_close,"
					+"       c_l_s_open,"
					+"       c_l_s_close,"
					+"       changeid"
					+"  from t_c_tradeauth_rt"
					+"  where firmid = ? and commodityid = 'SYSG100S' ";
			auth_params = new Object[]{privilege.getFirmId()};
			authRt_List = getJdbcTemplate().queryForList(auth_sql,
					auth_params);
		}

		List<Map> rt_List = getJdbcTemplate().queryForList(rt_sql,
				params);
		
		if (null != rt_List && rt_List.size() > 0) {
			Map firm_MarginRate = new HashMap();
			Map firm_FeeRate = new HashMap();
			Map firmTrade_Privilege = new HashMap();
			Map orderPointMap = new HashMap();
			Map holdQtytMap = new HashMap();
			String firmID = privilege.getFirmId();

			for (Map map : rt_List) {
				String commodityID = (String) map.get("CommodityID");
				// 设置交易商特殊保证金比例
				Margin_RT margin_RT = new Margin_RT();

				margin_RT.setCommodityID(commodityID);
				margin_RT.setFirmID(firmID);
				margin_RT.setHolidayMargin(((BigDecimal) map.get("HolidayMargin"))
						.doubleValue());
				margin_RT.setMarginAlgr(((BigDecimal) map.get("MarginAlgr"))
						.shortValue());
				margin_RT.setSettleMargin(((BigDecimal) map.get("SettleMargin"))
						.doubleValue());
				margin_RT.setTradeMargin(((BigDecimal) map.get("TradeMargin"))
						.doubleValue());

				firm_MarginRate.put(commodityID, margin_RT);
				// 设置交易商特殊手续费率
				Fee_RT fee_RT = new Fee_RT();

				fee_RT.setCommodityID(commodityID);
				fee_RT.setFirmID(firmID);
				fee_RT.setFeeAlgr(((BigDecimal) map.get("FeeAlgr")).shortValue());
				fee_RT.setFeeMode(((BigDecimal) map.get("FeeMode")).shortValue());
				fee_RT.setFeeRate(((BigDecimal) map.get("FeeRate")).doubleValue());
				fee_RT.setCcpFeeRate(((BigDecimal) map.get("CcpFeeRate")).doubleValue());

				firm_FeeRate.put(commodityID, fee_RT);


				// 设置交易商没有显示权限的商品
				if (0 == ((BigDecimal) map.get("Display")).intValue()) {
					privilege.getNoDisplayPrivilege().put(commodityID, null);
				}


				// 设置交易商权限
				TradeAuth_RT tradeAuth_RT = new TradeAuth_RT();
				if(authRt_List != null && authRt_List.size() > 0 && "SYSG100S".equals(commodityID)){ //如果是大合约商品，且t_c_tradeauth_rt表有配置项，则以t_c_tradeauth_rt表为准
					Map auth_map = authRt_List.get(0);
					tradeAuth_RT.setB_StopLoss(((BigDecimal) auth_map.get("B_StopLoss")).intValue());
					tradeAuth_RT.setB_StopProfit(((BigDecimal) auth_map.get("B_StopProfit")).intValue());
					tradeAuth_RT.setCancel_L_Open(((BigDecimal) auth_map.get("Cancel_L_Open")).intValue());
					tradeAuth_RT.setCancel_StopLoss(((BigDecimal) auth_map.get("Cancel_StopLoss")).intValue());
					tradeAuth_RT.setCancel_StopProfit(((BigDecimal) auth_map.get("Cancel_StopProfit")).intValue());
					tradeAuth_RT.setCommodityID(commodityID);
					tradeAuth_RT.setFirmID(firmID);
					tradeAuth_RT.setL_B_Open(((BigDecimal) auth_map.get("L_B_Open")).intValue());
					tradeAuth_RT.setL_S_Open(((BigDecimal) auth_map.get("L_S_Open")).intValue());
					tradeAuth_RT.setM_B_Close(((BigDecimal) auth_map.get("M_B_Close")).intValue());
					tradeAuth_RT.setM_B_Open(((BigDecimal) auth_map.get("M_B_Open")).intValue());
					tradeAuth_RT.setM_S_Close(((BigDecimal) auth_map.get("M_S_Close")).intValue());
					tradeAuth_RT.setM_S_Open(((BigDecimal) auth_map.get("M_S_Open")).intValue());
					tradeAuth_RT.setS_StopLoss(((BigDecimal) auth_map.get("S_StopLoss")).intValue());
					tradeAuth_RT.setS_StopProfit(((BigDecimal) auth_map.get("S_StopProfit")).intValue());
					tradeAuth_RT.setC_L_B_O(((BigDecimal) auth_map.get("C_L_B_OPEN")).intValue());
					tradeAuth_RT.setC_L_B_C(((BigDecimal) auth_map.get("C_L_B_CLOSE")).intValue());
					tradeAuth_RT.setC_L_C_O(((BigDecimal) auth_map.get("C_L_S_OPEN")).intValue());
					tradeAuth_RT.setC_L_C_C(((BigDecimal) auth_map.get("C_L_S_CLOSE")).intValue());				
				}else{
					tradeAuth_RT.setB_StopLoss(((BigDecimal) map.get("B_StopLoss")).intValue());
					tradeAuth_RT.setB_StopProfit(((BigDecimal) map.get("B_StopProfit")).intValue());
					tradeAuth_RT.setCancel_L_Open(((BigDecimal) map.get("Cancel_L_Open")).intValue());
					tradeAuth_RT.setCancel_StopLoss(((BigDecimal) map.get("Cancel_StopLoss")).intValue());
					tradeAuth_RT.setCancel_StopProfit(((BigDecimal) map.get("Cancel_StopProfit")).intValue());
					tradeAuth_RT.setCommodityID(commodityID);
					tradeAuth_RT.setFirmID(firmID);
					tradeAuth_RT.setL_B_Open(((BigDecimal) map.get("L_B_Open")).intValue());
					tradeAuth_RT.setL_S_Open(((BigDecimal) map.get("L_S_Open")).intValue());
					tradeAuth_RT.setM_B_Close(((BigDecimal) map.get("M_B_Close")).intValue());
					tradeAuth_RT.setM_B_Open(((BigDecimal) map.get("M_B_Open")).intValue());
					tradeAuth_RT.setM_S_Close(((BigDecimal) map.get("M_S_Close")).intValue());
					tradeAuth_RT.setM_S_Open(((BigDecimal) map.get("M_S_Open")).intValue());
					tradeAuth_RT.setS_StopLoss(((BigDecimal) map.get("S_StopLoss")).intValue());
					tradeAuth_RT.setS_StopProfit(((BigDecimal) map.get("S_StopProfit")).intValue());
					tradeAuth_RT.setC_L_B_O(((BigDecimal) map.get("C_L_B_OPEN")).intValue());
					tradeAuth_RT.setC_L_B_C(((BigDecimal) map.get("C_L_B_CLOSE")).intValue());
					tradeAuth_RT.setC_L_C_O(((BigDecimal) map.get("C_L_S_OPEN")).intValue());
					tradeAuth_RT.setC_L_C_C(((BigDecimal) map.get("C_L_S_CLOSE")).intValue());					
				}

				firmTrade_Privilege.put(commodityID, tradeAuth_RT);

				if(!"2".equals(privilege.getFirmType())){
					// 设置交易商特殊委托点差
					OrderPoint_RT orderPoint_RT = new OrderPoint_RT();

					orderPoint_RT.setCommodityID(commodityID);
					orderPoint_RT.setFirmID(firmID);
					orderPoint_RT.setL_Open_Point(((BigDecimal) map.get("L_Open_Point")).doubleValue());
					orderPoint_RT.setM_OrderPoint(((BigDecimal) map.get("M_OrderPoint")).doubleValue());
					orderPoint_RT.setMax_M_OrderPoint(((BigDecimal) map.get("Max_M_OrderPoint")).doubleValue());
					orderPoint_RT.setMin_M_OrderPoint(((BigDecimal) map.get("Min_M_OrderPoint")).doubleValue());
					orderPoint_RT.setStopLossPoint(((BigDecimal) map.get("StopLossPoint")).doubleValue());
					orderPoint_RT.setStopProfitPoint(((BigDecimal) map.get("StopProfitPoint")).doubleValue());

					orderPointMap.put(commodityID, orderPoint_RT);
				}
				// 设置交易商特殊委托数量
				HoldQty_RT holdQty_RT = new HoldQty_RT();

				holdQty_RT.setCommodityID(commodityID);
				holdQty_RT.setFirmID(firmID);
				holdQty_RT.setOneMaxOrderQty(((BigDecimal) map.get("OneMaxOrderQty")).longValue());
				holdQty_RT.setOneMinOrderQty(((BigDecimal) map.get("OneMinOrderQty")).doubleValue());
				holdQty_RT.setMaxNetHold(((BigDecimal) map.get("MaxNetHold")).longValue());
				holdQty_RT.setMaxHoldQty(((BigDecimal) map.get("MaxHoldQty")).longValue());

				holdQtytMap.put(commodityID, holdQty_RT);
			}

			privilege.setFirm_MarginRate(firm_MarginRate);
			privilege.setFirm_FeeRate(firm_FeeRate);
			privilege.setFirmTradePrivilege(firmTrade_Privilege);
			privilege.setOrderPoint(orderPointMap);
			privilege.setHoldQty(holdQtytMap);
		}

		// 默认会员代码 如果从会员信息中查不到特殊值 则通过默认会员代码查询
		String defFirmCode;
		if ("0".equals(privilege.getFirmType())) {
			defFirmCode = "Def_Member";
		} else {
			defFirmCode = "Def_S_Member";
		}

		// 设置交易商特殊延期费率
		String delayFee_sql = "select a.CommodityID, a.DelayFeeAlgr, " 
				+ "b.StepNo, b.DelayFee, c.lowvalue, c.stepvalue " 
				+ "from T_Commodity_RT a, T_C_DelayFee_RT b, T_A_StepDictionary c "
				+ "where a.Status = 1 and a.CommodityID = b.CommodityID "
				+ "and b.StepNo = c.StepNo and c.laddercode = 'DelayDays' and b.FirmID = ?";
		List<Map> delayFee_List = getJdbcTemplate().queryForList(delayFee_sql, 
				new Object[]{privilege.getM_FirmID()});
		Map mapPersonal = new HashMap(); // 哪些商品有个性化参数
		if (null != delayFee_List && delayFee_List.size() > 0) {
			Map firm_DelayFee = new HashMap();

			for (Map map : delayFee_List) {
				String commodityID = (String) map.get("CommodityID");
				DelayFee_RT delayFee_RT = (DelayFee_RT) firm_DelayFee.get(commodityID);
				if (null == delayFee_RT) {
					delayFee_RT = new DelayFee_RT();
					delayFee_RT.setCommodityID(commodityID);
					delayFee_RT.setDelayFeeAlgr(((BigDecimal) map.get("DelayFeeAlgr")).intValue());

					firm_DelayFee.put(commodityID, delayFee_RT);

					mapPersonal.put(commodityID, null);
				}

				List listDelayFee_Step_RT = delayFee_RT.getDelayFeeStepRT();
				if (null == listDelayFee_Step_RT) {
					listDelayFee_Step_RT = new ArrayList();
					delayFee_RT.setDelayFeeStepRT(listDelayFee_Step_RT);
				}

				DelayFee_Step_RT delayFee_Step_RT = new DelayFee_Step_RT();
				delayFee_Step_RT.setCommodityID(commodityID);
				delayFee_Step_RT.setFirmID(privilege.getM_FirmID());
				delayFee_Step_RT.setStepNo(((BigDecimal) map.get("StepNo")).intValue());
				delayFee_Step_RT.setDelayFee(((BigDecimal) map.get("DelayFee")).doubleValue());
				delayFee_Step_RT.setStepValue(((BigDecimal) map.get("stepvalue")).longValue());
				delayFee_Step_RT.setLowValue(((BigDecimal) map.get("lowvalue")).longValue());

				listDelayFee_Step_RT.add(delayFee_Step_RT);
			}

			privilege.setFirm_DelayFee(firm_DelayFee);
		}

		List<Map> delayFee_List_def = null;
		// 如果从 firmID中查询不到延期费信息或者少了商品, 则从默认用户中查询,得到没有个性化的延期费
		if (null == delayFee_List || delayFee_List.size() < rt_List.size()) {
			delayFee_List_def = getJdbcTemplate().queryForList(delayFee_sql, 
					new Object[]{defFirmCode});
		}

		if (null != delayFee_List_def && delayFee_List_def.size() > 0) {
			Map firm_DelayFee = privilege.getFirm_DelayFee();
			if (null == firm_DelayFee) {
				firm_DelayFee = new HashMap();
				privilege.setFirm_DelayFee(firm_DelayFee);
			}

			for (Map map : delayFee_List_def) {
				String commodityID = (String) map.get("CommodityID");
				if (mapPersonal.containsKey(commodityID)) { // 该商品延期费已经个性化了
					continue;
				}

				DelayFee_RT delayFee_RT = (DelayFee_RT) firm_DelayFee.get(commodityID);
				if (null == delayFee_RT) {
					delayFee_RT = new DelayFee_RT();
					delayFee_RT.setCommodityID(commodityID);
					delayFee_RT.setDelayFeeAlgr(((BigDecimal) map.get("DelayFeeAlgr")).intValue());

					firm_DelayFee.put(commodityID, delayFee_RT);
				}

				List listDelayFee_Step_RT = delayFee_RT.getDelayFeeStepRT();
				if (null == listDelayFee_Step_RT) {
					listDelayFee_Step_RT = new ArrayList();
					delayFee_RT.setDelayFeeStepRT(listDelayFee_Step_RT);
				}

				DelayFee_Step_RT delayFee_Step_RT = new DelayFee_Step_RT();
				delayFee_Step_RT.setCommodityID(commodityID);
				delayFee_Step_RT.setFirmID(privilege.getM_FirmID());
				delayFee_Step_RT.setStepNo(((BigDecimal) map.get("StepNo")).intValue());
				delayFee_Step_RT.setDelayFee(((BigDecimal) map.get("DelayFee")).doubleValue());
				delayFee_Step_RT.setStepValue(((BigDecimal) map.get("stepvalue")).longValue());
				delayFee_Step_RT.setLowValue(((BigDecimal) map.get("lowvalue")).longValue());

				listDelayFee_Step_RT.add(delayFee_Step_RT);
			}
		}

		checkMemberPrivilege(privilege);

		if(!"2".equals(privilege.getFirmType())){
			// 设置交易商特殊交易点差
			String quotePoint_sql = "select CommodityID,QuotePoint_B,QuotePoint_S, " 
					+ "QuotePoint_B_RMB,QuotePoint_S_RMB " 
					+ "from T_C_QuotePoint_RT where M_FirmID=?";

			log.debug("quotePoint_sql:" + quotePoint_sql);
			log.debug("Param1: M_FirmID= " + privilege.getM_FirmID());
			List<Map> quotePoint_List = getJdbcTemplate().queryForList(quotePoint_sql,
					new Object[]{privilege.getM_FirmID()});
			if (null != quotePoint_List && quotePoint_List.size() > 0) {
				Map quotePointMap = new HashMap();

				for (Map map : quotePoint_List) {
					String commodityID = (String) map.get("CommodityID");
					QuotePoint_RT quotePoint_RT = new QuotePoint_RT();

					quotePoint_RT.setM_FirmID(privilege.getM_FirmID());
					quotePoint_RT.setCommodityID(commodityID);
					quotePoint_RT.setQuotePoint_B(((BigDecimal) map.get("QuotePoint_B")).doubleValue());
					quotePoint_RT.setQuotePoint_S(((BigDecimal) map.get("QuotePoint_S")).doubleValue());
					quotePoint_RT.setQuotePoint_B_RMB(((BigDecimal) map.get("QuotePoint_B_RMB")).doubleValue());
					quotePoint_RT.setQuotePoint_S_RMB(((BigDecimal) map.get("QuotePoint_S_RMB")).doubleValue());

					quotePointMap.put(commodityID, quotePoint_RT);
				}
				privilege.setQuotePoint(quotePointMap);
			}

			// 如果是综合会员则设置自己的点差
			if ("1".equals(privilege.getFirmType())) {
				// 设置交易商特殊交易点差
				String my_QuotePoint_sql = "select CommodityID,QuotePoint_B,QuotePoint_S, " 
						+ "QuotePoint_B_RMB,QuotePoint_S_RMB " 
						+ "from T_C_QuotePoint_RT where M_FirmID=?";

				log.debug("my_QuotePoint_sql:" + my_QuotePoint_sql);
				log.debug("Param1: M_FirmID= " + privilege.getFirmId());
				List<Map> my_QuotePoint_List = getJdbcTemplate().queryForList(
						my_QuotePoint_sql, new Object[]{privilege.getFirmId()});
				if (my_QuotePoint_List != null && my_QuotePoint_List.size() > 0) {
					Map quotePointMap = new HashMap();
					for (Map map : my_QuotePoint_List) {
						String commodityID = (String) map.get("CommodityID");
						QuotePoint_RT quotePoint_RT = new QuotePoint_RT();

						quotePoint_RT.setM_FirmID(privilege.getM_FirmID());
						quotePoint_RT.setCommodityID(commodityID);
						quotePoint_RT.setQuotePoint_B(((BigDecimal) map.get("QuotePoint_B")).doubleValue());
						quotePoint_RT.setQuotePoint_S(((BigDecimal) map.get("QuotePoint_S")).doubleValue());
						quotePoint_RT.setQuotePoint_B_RMB(((BigDecimal) map.get("QuotePoint_B_RMB")).doubleValue());
						quotePoint_RT.setQuotePoint_S_RMB(((BigDecimal) map.get("QuotePoint_S_RMB")).doubleValue());

						quotePointMap.put(commodityID, quotePoint_RT);
					}
					privilege.setMyQuotePoint(quotePointMap);
				}
			}
		}


		return privilege;
	}

	// 检查会员权限 如果会员无权限则客户也无权限
	public void checkMemberPrivilege(Privilege privilege) {
		// 如果是客户 则先获取其所对应的综合会员权限..当综合会员没有权限时 客户也没有权限
		if ("0".equals(privilege.getFirmType())) {
			Map<String, TradeAuth_RT> otherFirmTrade_Privilege = new HashMap<String, TradeAuth_RT>();
			// 设置交易商权限
			String tradeAuth_sql = "Select t.CommodityID, t. Display, t.Cancel_L_Open, t.Cancel_StopLoss, t.Cancel_StopProfit, t.M_B_Open, " +
					"M_B_Close, t.L_B_Open, t.B_StopLoss, t.B_StopProfit, t.M_S_Open, t.M_S_Close, t.L_S_Open, t.S_StopLoss, t.S_StopProfit, " +
					"t.C_L_B_OPEN, t.C_L_B_CLOSE,t.C_L_S_OPEN, t.C_L_S_CLOSE " +
					" from T_C_TradeAuth_RT t where t.firmid = ? ";
			log.debug("sql:" + tradeAuth_sql);
			log.debug("Param1: FirmID= " + privilege.getM_FirmID());
			Object[] tradeAuth_param = new Object[]{privilege.getM_FirmID()};
			List<Map> tradeAuthList = getJdbcTemplate().queryForList(tradeAuth_sql,
					tradeAuth_param);
			if (null != tradeAuthList && tradeAuthList.size() > 0) {
				for (Map map : tradeAuthList) {
					// 设置交易商没有显示权限的商品
					if (0 == ((BigDecimal) map.get("Display")).intValue()) {
						privilege.getNoDisplayPrivilege().put((String) map.get("CommodityID"), null);
					}

					TradeAuth_RT tradeAuth_RT = new TradeAuth_RT();

					tradeAuth_RT.setB_StopLoss(((BigDecimal) map.get("B_StopLoss")).intValue());
					tradeAuth_RT.setB_StopProfit(((BigDecimal) map.get("B_StopProfit")).intValue());
					tradeAuth_RT.setCancel_L_Open(((BigDecimal) map.get("Cancel_L_Open")).intValue());
					tradeAuth_RT.setCancel_StopLoss(((BigDecimal) map.get("Cancel_StopLoss")).intValue());
					tradeAuth_RT.setCancel_StopProfit(((BigDecimal) map.get("Cancel_StopProfit")).intValue());
					tradeAuth_RT.setL_B_Open(((BigDecimal) map.get("L_B_Open")).intValue());
					tradeAuth_RT.setL_S_Open(((BigDecimal) map.get("L_S_Open")).intValue());
					tradeAuth_RT.setM_B_Close(((BigDecimal) map.get("M_B_Close")).intValue());
					tradeAuth_RT.setM_B_Open(((BigDecimal) map.get("M_B_Open")).intValue());
					tradeAuth_RT.setM_S_Close(((BigDecimal) map.get("M_S_Close")).intValue());
					tradeAuth_RT.setM_S_Open(((BigDecimal) map.get("M_S_Open")).intValue());
					tradeAuth_RT.setS_StopLoss(((BigDecimal) map.get("S_StopLoss")).intValue());
					tradeAuth_RT.setS_StopProfit(((BigDecimal) map.get("S_StopProfit")).intValue());
					tradeAuth_RT.setC_L_B_O(((BigDecimal) map.get("C_L_B_OPEN")).intValue());
					tradeAuth_RT.setC_L_B_C(((BigDecimal) map.get("C_L_B_CLOSE")).intValue());
					tradeAuth_RT.setC_L_C_O(((BigDecimal) map.get("C_L_S_OPEN")).intValue());
					tradeAuth_RT.setC_L_C_C(((BigDecimal) map.get("C_L_S_CLOSE")).intValue());

					otherFirmTrade_Privilege.put((String) map.get("CommodityID"), tradeAuth_RT);
				}
			}

			Map<String, TradeAuth_RT> firmTradePrivilege = privilege.getFirmTradePrivilege();
			if (otherFirmTrade_Privilege != null && firmTradePrivilege != null) {
				for (String commodityID : otherFirmTrade_Privilege.keySet()) {
					TradeAuth_RT otherFirmValueMap = (TradeAuth_RT) otherFirmTrade_Privilege.get(commodityID);
					TradeAuth_RT firmValueMap = firmTradePrivilege.get(commodityID);

					// 如果客户没有设置权限则使用会员权限
					if (null == firmValueMap) {
						firmTradePrivilege.put(commodityID, otherFirmValueMap);
					} else {
						if (0 == otherFirmValueMap.getM_B_Open()) {
							firmValueMap.setM_B_Open(otherFirmValueMap.getM_B_Open());
						}

						if (0 == otherFirmValueMap.getM_B_Close()) {
							firmValueMap.setM_B_Close(otherFirmValueMap.getM_B_Close());
						}

						if (0 == otherFirmValueMap.getM_S_Open()) {
							firmValueMap.setM_S_Open(otherFirmValueMap.getM_S_Open());
						}

						if (0 == otherFirmValueMap.getM_S_Close()) {
							firmValueMap.setM_S_Close(otherFirmValueMap.getM_S_Close());
						}

						if (0 == otherFirmValueMap.getC_L_B_O()) {
							firmValueMap.setC_L_B_O(otherFirmValueMap.getC_L_B_O());
						}

						if (0 == otherFirmValueMap.getC_L_B_C()) {
							firmValueMap.setC_L_B_C(otherFirmValueMap.getC_L_B_C());
						}
						if (0 == otherFirmValueMap.getC_L_C_O()) {
							firmValueMap.setC_L_C_O(otherFirmValueMap.getC_L_C_O());
						}

						if (0 == otherFirmValueMap.getC_L_C_C()) {
							firmValueMap.setC_L_C_C(otherFirmValueMap.getC_L_C_C());
						}


						if(0 == otherFirmValueMap.getM_B_Open() && 0 == otherFirmValueMap.getM_B_Close() && 0 == otherFirmValueMap.getM_S_Open() && 0 == otherFirmValueMap.getM_S_Close()){
							if(0 == otherFirmValueMap.getL_B_Open()){
								firmValueMap.setL_B_Open(otherFirmValueMap.getL_B_Open());
							}

							if(0 == otherFirmValueMap.getL_S_Open()){
								firmValueMap.setL_S_Open(otherFirmValueMap.getL_S_Open());
							}
						}

					}
				}
			}
		}
	}

}
