package com.kantoniak.discrete_fox.scene;

import android.support.v4.graphics.ColorUtils;

import com.kantoniak.discrete_fox.Country;
import com.kantoniak.discrete_fox.gameplay.Gameplay;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.TextureManager;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.scene.Scene;
import org.rajawali3d.util.ObjectColorPicker;

/**
 * Represents a single country on the map.
 * <p>
 * About 3D rendering: world map exists in a plane where X is to the right and Z is to top of the
 * map. Y axis is perpendicular to the map.
 */
public class CountryInstance {

    private static int COLOR_DEFAULT = 0xFFFAFAFA;
    private static int COLOR_DISABLED = 0xFFF5F5F5;
    private static int COLOR_BLACK = 0xFF000000;

    private static float BASE_HEIGHT = 0.1f;
    private static float Y_SCALE_MULTIPLIER = 0.5f;
    private static float NAME_Y_TRANSLATION = 0.1f;
    private static float NAME_SCALE = 0.1f;

    // State
    private final Country country;
    private int height = 0;
    private boolean disabled;
    private boolean visible = true;

    // Colors
    private int minColor = COLOR_DEFAULT;
    private int maxColor = COLOR_DEFAULT;

    // 3D
    private Object3D countryBase;
    private Object3D countryTop;
    private Plane countryName;
    private Material countryBaseMaterial = new Material();
    private Material countryTopMaterial = new Material();

    public CountryInstance(Country country) {
        this.country = country;
        this.disabled = true;
    }

    public void initMeshes(Object3D countryBase, Object3D countryTop, ATexture countryNameTexture) {
        this.countryBase = countryBase;
        this.countryTop = countryTop;

        countryBase.setDoubleSided(true);
        countryBaseMaterial.setColor(getBaseColor(COLOR_DEFAULT));
        countryBase.setMaterial(countryBaseMaterial);

        countryTop.setDoubleSided(true);
        countryTopMaterial.setColor(COLOR_DEFAULT);
        countryTop.setMaterial(countryTopMaterial);

        countryName = new Plane();
        countryName.setDoubleSided(true);
        countryName.setScale(NAME_SCALE);

        try {
            Material countryNameMaterial = new Material();
            countryNameMaterial.setColor(0x333333);
            countryNameMaterial.addTexture(TextureManager.getInstance().addTexture(countryNameTexture));
            countryName.setMaterial(countryNameMaterial);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
    }

    public void initPositions(Vector3 worldOffset, Vector2 countryMiddle) {
        countryBase.setPosition(worldOffset);
        countryTop.setPosition(worldOffset);

        countryName.setX(countryMiddle.getX() + worldOffset.x);
        countryName.setZ(-countryMiddle.getY() + worldOffset.z);
    }

    public void resetState() {
        height = 0;
        disabled = true;
        updateVisuals();
    }

    public void registerObject(Scene scene, ObjectColorPicker objectPicker) {
        scene.addChild(countryBase);
        scene.addChild(countryTop);
        scene.addChild(countryName);

        objectPicker.registerObject(countryBase);
        objectPicker.registerObject(countryTop);
        objectPicker.registerObject(countryName);
    }

    public boolean containsObject(final Object3D object) {
        return countryBase == object || countryTop == object || countryName == object;
    }

    public void setHeight(int height) {
        this.height = height;
        if (this.height > Gameplay.Settings.MAX_COUNTRY_HEIGHT) {
            this.height = 0;
        }
        updateVisuals();
    }

    public void onPicked() {
        if (disabled) {
            return;
        }
        setHeight(++height);
        updateVisuals();

        // TODO(kedzior) mp3 play height
        //MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.);
        //mp.start();
    }

    private void updateVisuals() {

        countryTop.setVisible(visible);
        countryBase.setVisible(visible);
        countryName.setVisible(visible);

        if (!visible) {
            return;
        }

        countryBase.setVisible(this.height > 0);
        countryName.setVisible(!this.disabled);

        if (height == 0) {
            countryTop.setY(0);
            countryTopMaterial.setColor(COLOR_DEFAULT);
            countryName.setY(NAME_Y_TRANSLATION);
        } else {
            countryBase.setScaleY(height * Y_SCALE_MULTIPLIER);
            countryTop.setY(BASE_HEIGHT * (float) height * Y_SCALE_MULTIPLIER);
            countryName.setY(BASE_HEIGHT * (float) height * Y_SCALE_MULTIPLIER + NAME_Y_TRANSLATION);

            float colorRatio = (height - 1) / (float) (Gameplay.Settings.MAX_COUNTRY_HEIGHT - 1);
            countryBaseMaterial.setColor(getBaseColor(ColorUtils.blendARGB(minColor, maxColor, colorRatio)));
            countryTopMaterial.setColor(ColorUtils.blendARGB(minColor, maxColor, colorRatio));
        }

        if (disabled) {
            countryBaseMaterial.setColor(getBaseColor(COLOR_DISABLED));
            countryTopMaterial.setColor(COLOR_DISABLED);
        }

    }

    private int getBaseColor(int topColor) {
        return ColorUtils.blendARGB(COLOR_BLACK, topColor, 0.5f);
    }

    public int getHeight() {
        return height;
    }

    public Country getCountry() {
        return country;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        updateVisuals();
    }

    public void setColors(int minColor, int maxColor) {
        this.minColor = minColor;
        this.maxColor = maxColor;
        updateVisuals();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        updateVisuals();
    }
}
