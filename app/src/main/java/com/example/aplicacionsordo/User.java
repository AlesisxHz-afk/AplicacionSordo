package com.example.aplicacionsordo;

import java.io.Serializable;

public class User implements Serializable {
    private int usuarioId;
    private String usuario;
    private String nombres;
    private String apePaterno;
    private String apeMaterno;
    private boolean estadoUsuario;
    private String profilePhotoUri;

    public User(int usuarioId, String usuario, String nombres, String apePaterno, String apeMaterno, boolean estadoUsuario) {
        this(usuarioId, usuario, nombres, apePaterno, apeMaterno, estadoUsuario, null);
    }

    public User(int usuarioId, String usuario, String nombres, String apePaterno, String apeMaterno, boolean estadoUsuario, String profilePhotoUri) {
        this.usuarioId = usuarioId;
        this.usuario = usuario;
        this.nombres = nombres;
        this.apePaterno = apePaterno;
        this.apeMaterno = apeMaterno;
        this.estadoUsuario = estadoUsuario;
        this.profilePhotoUri = profilePhotoUri;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApePaterno() {
        return apePaterno;
    }

    public String getApeMaterno() {
        return apeMaterno;
    }

    public String getProfilePhotoUri() {
        return profilePhotoUri;
    }

    public void setProfilePhotoUri(String profilePhotoUri) {
        this.profilePhotoUri = profilePhotoUri;
    }

    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();
        if (nombres != null) sb.append(nombres).append(" ");
        if (apePaterno != null) sb.append(apePaterno).append(" ");
        if (apeMaterno != null) sb.append(apeMaterno);
        return sb.toString().trim();
    }

    public boolean isEstadoUsuario() {
        return estadoUsuario;
    }
}
