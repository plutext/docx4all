<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:java="http://xml.apache.org/xalan/java" 
    xmlns:pkg="http://schemas.microsoft.com/office/2006/xmlPackage"
    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
  xmlns:xml="http://www.w3.org/XML/1998/namespace"                
  version="1.0" 
  exclude-result-prefixes="java"> 
  

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
  
<xsl:param name="chunkOnEachBlock"/>
<xsl:param name="mediatorInstance"/>

  <!--xsl:preserve-space elements="w:t"/-->

  <xsl:template match="/ | @*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>


  <xsl:template match="w:body">
    <w:body>
    <xsl:for-each select="*">

      <xsl:choose>
        <xsl:when test="local-name(.)='p'">
          <!-- have to wrap it in an SDT.
          
          That is all that is necessary if chunkOnEachBlock
          
          If we are chunking on H1, then it may be necessary to
          stick this w:p in the previous sdt (ie unless it is an H1).
          
          That's a TODO - which is probably better done via SAX.          
          
          A top level w:p occurs when a user types outside an SDT.
          
          If the user types a new w:p, that is what they get.
          
          But if they type on the same line as an existing sdt, it
          changes from a block sdt to an inline one.
          
          These 2 scenarios are handled here.
          -->
          <w:sdt>
            <w:sdtPr>
              <xsl:choose>
                <xsl:when test="count(w:sdt/w:sdtPr/w:id)>0">
                  <w:tag w:val="{w:sdt/w:sdtPr/w:tag/@w:val}"/>
                  <w:id  w:val="{w:sdt/w:sdtPr/w:id/@w:val}"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:variable name="generatedId"  select="java:org.plutext.client.Mediator.generateId()" />
                  <w:id  w:val="{$generatedId}" />
                  
                  <!--  Use extension function to get tag -->
				  <xsl:variable name="tag" 
					select="java:org.plutext.client.SdtWrapper.generateTag($generatedId, '0')" />					
                  <w:tag w:val="{$tag}"/>                  
                </xsl:otherwise>
              </xsl:choose>
            </w:sdtPr>
            <w:sdtContent>
              <xsl:apply-templates select="."  mode="dropSdt" />
            </w:sdtContent>
          </w:sdt>

        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="."/>          
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
    </w:body>
  </xsl:template>

  <xsl:template match="w:sdt" mode="dropSdt">

    <xsl:apply-templates select="w:sdtContent/*" />
    
  </xsl:template>

  <xsl:template match="@*|node()"  mode="dropSdt">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"  mode="dropSdt" />
    </xsl:copy>
  </xsl:template>



  <xsl:template match="w:sdt">
    <!-- This template is responsible for chunking,
    and also for handling the case where there are nested sdts.
    
    We don't try to be clever about using the sdt id of any
    nested sdt, but one could do so.  
    
    For example, if the first para contains a nested sdt, use
    that id as the id of the first sdt (and use the outside 
    sdt id for the 2nd resulting sdt).  If it was the second
    para which contained the nested sdt, then you'd use the
    nested sdt id on the 2nd resulting sdt).-->

    <xsl:variable name="id" select="w:sdtPr/w:id/@w:val" />

    <!--
    <xsl:variable name="runContents" select="w:sdtContent/w:p/w:r/w:t" />
     results in an XPathArrayIterator object -->

    <xsl:variable name="textContents" select="string(w:sdtContent)" />

    <xsl:choose>

      <xsl:when test="$chunkOnEachBlock=true() and count(w:sdtContent/*)>1
                and count(w:sdtContent/w:p[position()>1]/w:r/w:t/text()[string-length(.)!=0])>0">
        
        <!-- that last bit: only chunk if one of the subsequent paragraphs contains text
             ie it is not just there as a spacer .. 
             
             hmmm .. suppose there are 3 paras, only one of which is empty.  In this case
             we will create a chunk out of the spacer para.  Oh well, the user can use
             the merge button to fix this.
             
             -->

        <!-- and chunkOnEachBlock -->
        <xsl:for-each select="w:sdtContent/*">
          <xsl:choose>
            <xsl:when test="position()=1">
              <!-- make an Sdt with the existing ID,
                   and copy the first object into it -->
              <w:sdt>
                <w:sdtPr>
                  <w:tag w:val="{../../w:sdtPr/w:tag/@w:val}"/>
                  <w:id  w:val="{$id}" />
                </w:sdtPr>
                <w:sdtContent>
                  <xsl:apply-templates select="."  mode="dropSdt" />
                </w:sdtContent>
              </w:sdt>
              
            </xsl:when>
            <xsl:otherwise>

              <xsl:variable name="generatedId"  select="java:org.plutext.client.Mediator.generateId()" />

              <w:sdt>
                <w:sdtPr>
                  <w:id  w:val="{$generatedId}" />
                  
                  <!--  Use extension function to get tag -->
				  <xsl:variable name="tag" 
					select="java:org.plutext.client.SdtWrapper.generateTag($generatedId, '0')" />					
                  <w:tag w:val="{$tag}"/>                  
                  
                </w:sdtPr>
                <w:sdtContent>
                  <xsl:apply-templates select="."  mode="dropSdt" />
                </w:sdtContent>
              </w:sdt>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>        
      </xsl:when>

      <xsl:when test="java:org.plutext.client.Mediator.isDeletedPermanently($mediatorInstance, string($id), $textContents ) and (count(w:sdtContent/w:p/w:del) + count(w:sdtContent/w:p/w:ins)=0)">
        <!-- The count stuff is necessary, because the extension function
             only gets the text contents.  It doesn't know whether the 
             text contents is a normal run, or one in a w:del 
             
             Don't really need the count bit, because we only count if
             all we have is whitespace; however, whitespace in a w:ins
             or w:del probably should not be deleted.  -->  
        <!-- Drop it-->
      </xsl:when>

      <xsl:otherwise>
        <xsl:copy-of select="."/>        
      </xsl:otherwise>
      
    </xsl:choose>
    
    
    
  </xsl:template>
  


</xsl:stylesheet>