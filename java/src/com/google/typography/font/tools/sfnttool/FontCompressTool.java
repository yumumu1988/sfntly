package com.google.typography.font.tools.sfnttool;

import com.google.typography.font.sfntly.Font;
import com.google.typography.font.sfntly.FontFactory;
import com.google.typography.font.sfntly.Tag;
import com.google.typography.font.sfntly.table.core.CMapTable;
import com.google.typography.font.tools.subsetter.RenumberingSubsetter;
import com.google.typography.font.tools.subsetter.Subsetter;

import java.io.*;
import java.util.*;

public class FontCompressTool {

    public byte[] compressFontByte(String fontFilePath, String words) throws IOException {
        FontFactory fontFactory = FontFactory.getInstance();
        File file = new File(fontFilePath);
        try (FileInputStream fileInputStream = new FileInputStream(file);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ) {
            byte[] fontBytes = new byte[(int) file.length()];
            fileInputStream.read(fontBytes);
            Font[] fontArray = fontFactory.loadFonts(fontBytes);
            return compressFont(byteArrayOutputStream, fontFactory, fontArray, words);
        }
    }

    public byte[] compressFontByte(byte[] fontBytes, String words) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fontBytes)) {
            FontFactory fontFactory = FontFactory.getInstance();
            Font[] fontArray = fontFactory.loadFonts(byteArrayInputStream);
            return compressFont(byteArrayOutputStream, fontFactory, fontArray, words);
        }
    }

    public byte[] compressFontByte(InputStream fontStream, String words) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            FontFactory fontFactory = FontFactory.getInstance();
            Font[] fontArray = fontFactory.loadFonts(fontStream);
            return compressFont(byteArrayOutputStream, fontFactory, fontArray, words);
        }
    }

    private byte[] compressFont(ByteArrayOutputStream byteArrayOutputStream, FontFactory fontFactory, Font[] fontArray, String words) throws IOException {
        Font font = fontArray[0];
        List<CMapTable.CMapId> cMapIdList = new ArrayList<>();
        cMapIdList.add(CMapTable.CMapId.WINDOWS_BMP);

        Subsetter subsetter = new RenumberingSubsetter(font, fontFactory);
        subsetter.setCMaps(cMapIdList, 1);
        List<Integer> glyphs = GlyphCoverage.getGlyphCoverage(font, words);
        subsetter.setGlyphs(glyphs);
        Set<Integer> removeTables = new HashSet<>();
        // Most of the following are valid tables, but we don't renumber them yet, so strip
        removeTables.add(Tag.GDEF);
        removeTables.add(Tag.GPOS);
        removeTables.add(Tag.GSUB);
        removeTables.add(Tag.kern);
        removeTables.add(Tag.hdmx);
        removeTables.add(Tag.vmtx);
        removeTables.add(Tag.VDMX);
        removeTables.add(Tag.LTSH);
        removeTables.add(Tag.DSIG);
        removeTables.add(Tag.vhea);
        // AAT tables, not yet defined in sfntly Tag class
        removeTables.add(Tag.intValue(new byte[]{'m', 'o', 'r', 't'}));
        removeTables.add(Tag.intValue(new byte[]{'m', 'o', 'r', 'x'}));
        subsetter.setRemoveTables(removeTables);
        font = subsetter.subset().build();
        fontFactory.serializeFont(font, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
