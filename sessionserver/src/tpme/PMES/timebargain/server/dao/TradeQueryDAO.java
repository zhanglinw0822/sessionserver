package tpme.PMES.timebargain.server.dao;

import tpme.PMES.timebargain.server.model.Privilege;

/**
 * 将查询权限独立出来
 * @author wangy
 *
 */
public interface TradeQueryDAO {

	Privilege getTradePrivilege(Privilege privilege);
}
