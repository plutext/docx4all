<?xml version="1.0" encoding="UTF-8" ?>
  <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    xmlns:o="urn:schemas-microsoft-com:office:office"
    xmlns:v="urn:schemas-microsoft-com:vml"
    xmlns:WX="http://schemas.microsoft.com/office/word/2003/auxHint"
    xmlns:aml="http://schemas.microsoft.com/aml/2001/core"
    xmlns:w10="urn:schemas-microsoft-com:office:word"
    xmlns:pkg="http://schemas.microsoft.com/office/2006/xmlPackage"
          xmlns:msxsl="urn:schemas-microsoft-com:xslt"
      xmlns:ext="http://www.xmllab.net/wordml2html/ext"
    xmlns:java="http://xml.apache.org/xalan/java"
    xmlns:xml="http://www.w3.org/XML/1998/namespace"
    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    version="1.0"
          exclude-result-prefixes="java msxsl ext o v WX aml w10">

    <!-- 
  *  Copyright 2007, Plutext Pty Ltd.
  *
  *  This file is part of plutext-client-word2007.

  plutext-client-word2007 is free software: you can redistribute it and/or
  modify it under the terms of version 3 of the GNU General Public License
  as published by the Free Software Foundation.

  plutext-client-word2007 is distributed in the hope that it will be
  useful, but WITHOUT ANY WARRANTY; without even the implied warranty
  of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with plutext-client-word2007.  If not, see
  <http://www.gnu.org/licenses/>.

  -->    

    <xsl:output method="xml" encoding="utf-8" omit-xml-declaration="no" indent="yes" />

    <xsl:template match="/pkg:package">
      <ReferenceMap>

          <xsl:apply-templates select="pkg:part/pkg:xmlData/w:document/w:body/w:sdt"/>

      </ReferenceMap>
    </xsl:template>

    <xsl:template match="/">
      <xsl:apply-templates select="*"/>
    </xsl:template>    

    <xsl:template match="@*|node()">
      <xsl:copy>
        <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
    </xsl:template>

        
    <xsl:template match="w:sdt">
      <sdt>
        <id>
        <xsl:value-of select="w:sdtPr/w:id/@w:val"/>
      </id>
      <rels>
        <xsl:for-each select=".//@r:embed | .//@r:link | .//@r:id">
          <idref>
            <xsl:value-of select="."/>
          </idref>          
        </xsl:for-each>
      </rels>
      <comments>
        <xsl:for-each select=".//w:commentReference[count(@w:id)=1]">
          <!-- Require @w:id to exist, since there was an odd example
               of <commentReference/> in an SDT with commentRangeStart and commentRangeEnd,
               but the real <commentReference w:id=..> being in the next SDT!!! -->
          <idref>
            <xsl:value-of select="@w:id"/>
          </idref>
        </xsl:for-each>
      </comments>
      <footnotes>
        <xsl:for-each select=".//w:footnoteReference">
          <idref>
            <xsl:value-of select="@w:id"/>
          </idref>
        </xsl:for-each>
      </footnotes>
      <endnotes>
        <xsl:for-each select=".//endnoteReference">
          <idref>
            <xsl:value-of select="@w:id"/>
          </idref>
        </xsl:for-each>
      </endnotes>
      </sdt>

    </xsl:template>
    
  </xsl:stylesheet>