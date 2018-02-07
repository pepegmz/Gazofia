package com.gazofiadevelopers.gazofiaapp.data;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by eitdev on 07/02/18.
 */

public class CustomMark {
    private String id;
    private String type;
    private LatLng latLng;
    private String premium;
    private String magna;
    private String diesel;
    private List<Object> services;
    private String colonia;
    private String calle;
    private String municipio;
    private String name;

    public CustomMark(String id, String type, LatLng latLng, String premium, String magna, String diesel, List<Object> services, String colonia, String calle, String municipio, String name) {
        this.id = id;
        this.type = type;
        this.latLng = latLng;
        this.premium = premium;
        this.magna = magna;
        this.diesel = diesel;
        this.services = services;
        this.colonia = colonia;
        this.calle = calle;
        this.municipio = municipio;
        this.name = name;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public String getPremium() {
        return premium;
    }

    public void setPremium(String premium) {
        this.premium = premium;
    }

    public String getMagna() {
        return magna;
    }

    public void setMagna(String magna) {
        this.magna = magna;
    }

    public String getDiesel() {
        return diesel;
    }

    public void setDiesel(String diesel) {
        this.diesel = diesel;
    }

    public List<Object> getServices() {
        return services;
    }

    public void setServices(List<Object> services) {
        this.services = services;
    }

    public String getColonia() {
        return colonia;
    }

    public void setColonia(String colonia) {
        this.colonia = colonia;
    }

    public String getCalle() {
        return calle;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
