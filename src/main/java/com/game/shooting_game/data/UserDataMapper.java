package com.game.shooting_game.data;

import com.game.shooting_game.entity.UserEntity;
import com.game.shooting_game.entity.UserShipEntity;
import com.game.shooting_game.utils.LogUtils;

/**
 * DBのエンティティ(UserEntity)とゲーム中のデータ(UserData)の相互変換
 */
public final class UserDataMapper {
    private static final String component = "[UserDataMapper]";

    private UserDataMapper() {
    }

    /**
     * UserEntityからUserDataへの変換
     * 
     * @param entity UserEntityオブジェクト
     * @return
     */
    public static UserData toUserData(UserEntity entity) {
        UserData data = new UserData();

        // 基本情報のコピー
        data.setUsername(entity.getUsername());
        data.setLastPlayTime(entity.getLastPlayTime());
        data.setTotalPlayTime(entity.getTotalPlayTime());
        data.setSelectedShip(entity.getSelectedShip());
        data.setSubShip(entity.getSubShip());
        if (entity.getSubShipParts() != null)
            data.getSubShipParts().putAll(entity.getSubShipParts());

        // モジュールのコピー(DBにはJSON文字列で保存している)
        if (entity.getModuleData() != null && !entity.getModuleData().isBlank()) {
            try {
                data.setModules(new com.fasterxml.jackson.databind.ObjectMapper().readValue(entity.getModuleData(),
                        com.game.shooting_game.domain.module.ModuleInventory.class));
            } catch (Exception e) {
                LogUtils.systemLog("[ERROR]", component, "Failed to read module data.");
            }
        }

        // 進捗のコピー
        data.getProgress().setExp(entity.getExp());
        data.getProgress().setClearedStage(entity.getClearedStage());
        data.getProgress().setHighScores(entity.getHighScores());

        // 資源のコピー
        data.getResources().setCoins(entity.getCoins());
        data.getResources().setEquipments(entity.getEquipments());

        // 船の情報のコピー
        entity.getOwnedShips().forEach(shipEntity -> {
            String shipId = shipEntity.getShipId();
            data.unlockShip(shipId);

            // アップグレード情報のコピー
            if (shipEntity.getUpgrades() != null) {
                data.getShips().get(shipId).getUpgrades().putAll(shipEntity.getUpgrades());
            }
            UserData.ShipStats st = data.getShips().get(shipId);
            st.setLevel(Math.max(1, shipEntity.getUnitLevel())); // 0(未設定)は1として扱う
            st.setStars(Math.max(0, shipEntity.getStars()));
            st.setCopies(Math.max(0, shipEntity.getCopies()));
            st.setResearchSkill(shipEntity.getResearchSkill());
            st.setResearchFinishAt(shipEntity.getResearchFinishAt());
        });

        return data;
    }

    /**
     * UserDataからUserEntityへの変換と同期
     * 
     * @param entity
     */
    public static void copyToEntity(UserData userData, UserEntity entity) {
        // 基本フィールド
        entity.setLastPlayTime(userData.getLastPlayTime());
        entity.setTotalPlayTime(userData.getTotalPlayTime());
        entity.setSelectedShip(userData.getSelectedShip());
        entity.setSubShip(userData.getSubShip());
        entity.getSubShipParts().clear();
        if (userData.getSubShipParts() != null)
            entity.getSubShipParts().putAll(userData.getSubShipParts());

        // モジュール
        try {
            entity.setModuleData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(userData.getModules()));
        } catch (Exception e) {
            LogUtils.systemLog("[ERROR]", component, "Failed to write module data.");
        }

        // 進捗
        entity.setExp(userData.getProgress().getExp());
        entity.setClearedStage(userData.getProgress().getClearedStage());
        entity.getHighScores().clear();
        if (userData.getProgress().getHighScores() != null) {
            entity.getHighScores().putAll(userData.getProgress().getHighScores());
        }

        // 資源
        entity.setCoins(userData.getResources().getCoins());
        entity.getEquipments().clear();
        if (userData.getResources().getEquipments() != null) {
            entity.getEquipments().putAll(userData.getResources().getEquipments());
        }

        // 船の情報
        entity.getOwnedShips().clear();
        userData.getShips().forEach((shipId, stats) -> {
            UserShipEntity shipEntity = new UserShipEntity();
            shipEntity.setShipId(shipId);
            shipEntity.setUnitLevel(stats.getLevel());
            shipEntity.setStars(stats.getStars());
            shipEntity.setCopies(stats.getCopies());
            shipEntity.setResearchSkill(stats.getResearchSkill());
            shipEntity.setResearchFinishAt(stats.getResearchFinishAt());

            if (stats.getUpgrades() != null) {
                shipEntity.getUpgrades().putAll(stats.getUpgrades());
            }
            entity.getOwnedShips().add(shipEntity);
        });
    }
}
