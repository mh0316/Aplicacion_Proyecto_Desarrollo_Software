package com.example.apptransito

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "AppTransito.db"
        private const val DATABASE_VERSION = 2 // Incrementa la versión
        private const val TABLE_USERS = "usuarios"
        private const val COLUMN_RUT = "rut"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_NOMBRE = "nombre"
        private const val COLUMN_APELLIDO = "apellido"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = "CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_RUT TEXT PRIMARY KEY, " +
                "$COLUMN_PASSWORD TEXT, " +
                "$COLUMN_EMAIL TEXT, " +
                "$COLUMN_NOMBRE TEXT, " +
                "$COLUMN_APELLIDO TEXT)"
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Migración para agregar nuevos campos
            db?.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_EMAIL TEXT")
            db?.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_NOMBRE TEXT")
            db?.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_APELLIDO TEXT")
        }
    }

    // Insertar nuevo usuario con todos los campos
    fun insertUser(rut: String, password: String, email: String, nombre: String, apellido: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_RUT, rut)
        contentValues.put(COLUMN_PASSWORD, password)
        contentValues.put(COLUMN_EMAIL, email)
        contentValues.put(COLUMN_NOMBRE, nombre)
        contentValues.put(COLUMN_APELLIDO, apellido)

        val result = db.insert(TABLE_USERS, null, contentValues)
        return result != -1L
    }

    // Verificar si el usuario existe
    fun checkUser(rut: String, password: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_RUT = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(rut, password))
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    // Verificar si el RUT ya está registrado
    fun userExists(rut: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_RUT = ?"
        val cursor = db.rawQuery(query, arrayOf(rut))
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    // Verificar si el email ya está registrado
    fun emailExists(email: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email))
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    // Actualizar contraseña de un usuario
    fun updatePassword(rut: String, newPassword: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_PASSWORD, newPassword)

        val result = db.update(TABLE_USERS, contentValues, "$COLUMN_RUT = ?", arrayOf(rut))
        return result > 0
    }
}