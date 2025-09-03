package com.example.tabooladisplayapp.presentation.service;

interface ICellColorService {
    boolean updateCellBackgroundColor(int position, String colorHex, boolean isVisible, String securityToken);
}