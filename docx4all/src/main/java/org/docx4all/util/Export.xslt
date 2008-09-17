<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:myObj="urn:myObj"
    xmlns:pkg="http://schemas.microsoft.com/office/2006/xmlPackage"
    xmlns:ns2="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    xmlns:ns4="http://schemas.openxmlformats.org/schemaLibrary/2006/main"
    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
  xmlns:xml="http://www.w3.org/XML/1998/namespace"                
  version="1.0" >	
  

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

  <xsl:output method="xml" encoding="utf-8" omit-xml-declaration="no" 
  indent="yes"/>  
  

  <xsl:template match="/ | @*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="w:sdt">

    <xsl:apply-templates select="w:sdtContent/*" />
    
  </xsl:template>



</xsl:stylesheet>