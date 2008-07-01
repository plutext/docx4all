using System;
using System.Collections.Generic;
using System.Text;
using Word = Microsoft.Office.Interop.Word;
using log4net;
using System.Threading;


namespace plutext.client.word2007
{
    public class Chunker
    {

        private static readonly ILog log = LogManager.GetLogger(typeof(Chunker));

        public static Boolean containsMultipleBlocks(Word.ContentControl cc)
        {

              Word.Range r = cc.Range;

              if (r.Paragraphs.Count > 1)
              {
                  return true;
              }

              return false;

              // foreach ( Word.Paragraph p in r.Paragraphs)
    

        }


        /* Split a control containing n paragraphs
         * into n controls.  
         * 
         * The ID of the first control remains the same.
         * 
         * Returns a list of the IDs of the new controls. */
        public static List<String> chunk(Word.ContentControl cc)
        {

            List<String> newChunkIDs = new List<String>();

            log.Debug("Detected multiple para: " + cc.ID);

            selectAfter(cc);

            // Define the selection point
            Word.Selection selection = Globals.ThisAddIn.Application.Selection;

            Boolean first = true;
            foreach (Word.Paragraph p in cc.Range.Paragraphs)
            {
                // Don't copy the first paragraph into a new control
                if (first)
                {
                    first = false;
                    continue;
                }

                //Word.Range newR = p.Range.Duplicate;
                p.Range.Copy();

                object oRng = (object)selection.Range;

                Word.ContentControl newCC = Globals.ThisAddIn.Application.ActiveDocument.ContentControls
                    .Add(Word.WdContentControlType.wdContentControlRichText,
                            ref  oRng);

                newChunkIDs.Add(newCC.ID);

                log.Debug("Inserted new sdt: " + newCC.ID);

                newCC.Tag = "0"; // Version 0

                newCC.Range.Paste();

                // The paste results in an excess empty final para
                handleGhostParagraphMarker(newCC);

                selectAfter(newCC);

            }

            // Now remove the excess paragraphs from the first content control
            removeParagraphs(cc);
            // and hide the extra para marker
            handleGhostParagraphMarker(cc);

            return newChunkIDs;
        }

        /** The copied paragraph contains 2 paragraph markers: 
         * 
         * <w:sdtContent>
				<w:p>
					<w:r>
						<w:t>3</w:t>
					</w:r>
				</w:p>
				<w:p />
			</w:sdtContent>
         * 
         * Removing the 
         *  second deletes the content control!  So remove the first.  */
        public static void handleGhostParagraphMarker(Word.ContentControl cc)
        {
            object missing = Type.Missing;


            Word.Range r = cc.Range;

            log.Debug("range contains " + r.Paragraphs.Count);

            object oUnit = Word.WdUnits.wdCharacter;
            object oCount1 = 2;
            object oCountNeg = -1;
            object oCount2 = 2;

            // 2, -2, 1 makes the cc's inline
            // 2, -3, 2 removes the cc's entirely (but leaves 2 paragraph marks)

            r.Move(ref oUnit, ref oCount2);   // 2 past the end

            r.Move(ref oUnit, ref oCountNeg); // 3 backwards
            //r.Text = "#";  // Insert this between the 2 para marks

            r.MoveStart(ref oUnit, ref oCountNeg); // Add the first para mark to the selection


            r.Select();

            Word.Selection selection = Globals.ThisAddIn.Application.Selection;
            //selection.TypeBackspace();
            //selection.InsertParagraph();
            selection.InsertStyleSeparator();

            // r.MoveEnd(ref oUnit, ref oCountNeg); // just keeps the #, but both para marks gone!

            //r.Delete(ref missing, ref missing);

            //r.InsertParagraph(); // Gets us back to 2 para :(
        }


        /** Remove all the paragraphs from the content control, except
         *  the first. */
        public static void removeParagraphs(Word.ContentControl cc)
        {
            object missing = Type.Missing;

            
            Word.Range r = cc.Range;

            log.Debug("range contains " + r.Paragraphs.Count);

            //object oUnit = Word.WdUnits.wdParagraph;
            //object oCountSingle = 1;
            //r.MoveStart(ref oUnit, ref oCountSingle);

            if (r.Paragraphs.Count > 1)
            {

                r.Start = r.Paragraphs[2].Range.Start;

                log.Debug("now range contains " + r.Paragraphs.Count);

                //object oCount = deleteCount;
                r.Delete(ref missing, ref missing);
                log.Debug("deleted them");
            }

            //Boolean first = true;
            //foreach (Word.Paragraph p in cc.Range.Paragraphs)
            //{
            //    // Don't copy the first paragraph into a new control
            //    if (first)
            //    {
            //        first = false;
            //        continue;
            //    }

            //    p.Range.Delete(ref oUnit, ref oCount);
                
            //}

        }

        /** Move the selection point to the position immediately
         *  following this content control. */
        public static void selectAfter(Word.ContentControl cc)
        {

            Word.Range r = cc.Range;

            // r.Collapse (Word.wdCollapseEnd)
            
            object oUnit = Word.WdUnits.wdCharacter;            
            object oCount = 2;

            r.Move(ref oUnit, ref oCount);

            r.Select();
        }

    }
}
