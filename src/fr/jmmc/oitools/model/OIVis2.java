/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: OIVis2.java,v 1.4 2010-06-17 15:01:56 bourgesl Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2010/05/28 07:53:07  bourgesl
 * unified code to compute spacial coords
 *
 * Revision 1.2  2010/05/27 16:13:29  bourgesl
 * javadoc + small refactoring to expose getters/setters for keywords and getters for columns
 *
 * Revision 1.1  2010/04/28 14:47:37  bourgesl
 * refactored OIValidator classes to represent the OIFits data model
 *
 * Revision 1.9  2009/03/09 10:27:24  mella
 * Add spacialFreq and spacialCoord getter
 *
 * Revision 1.8  2008/10/28 08:36:19  mella
 * Add javadoc
 *
 * Revision 1.7  2008/04/08 14:22:16  mella
 * Include Evelyne comments
 *
 * Revision 1.6  2008/03/28 09:03:00  mella
 * Add AcceptedStaIndex for further checks
 *
 * Revision 1.5  2008/03/20 14:25:06  mella
 * First semantic step
 *
 * Revision 1.4  2008/03/18 13:23:04  mella
 * suppress common descs and inherit from oiData
 *
 * Revision 1.3  2008/03/13 07:25:48  mella
 * General commit after first keywords and columns definitions
 *
 * Revision 1.2  2008/03/11 14:48:52  mella
 * commit when evening is comming
 *
 * Revision 1.1  2008/02/28 08:10:40  mella
 * First revision
 *
 ******************************************************************************/
package fr.jmmc.oitools.model;

import fr.jmmc.oitools.OIFitsConstants;
import fr.jmmc.oitools.meta.ColumnMeta;
import fr.jmmc.oitools.meta.Types;
import fr.jmmc.oitools.meta.WaveColumnMeta;

/**
 * Class for OI_VIS2 table.
 */
public final class OIVis2 extends OIData {

  /** 
   * Public OIVis2 class constructor.
   * @param oifitsFile main OifitsFile
   */
  public OIVis2(final OIFitsFile oifitsFile) {
    super(oifitsFile);

    // VIS2DATA  column definition
    addColumnMeta(new WaveColumnMeta(OIFitsConstants.COLUMN_VIS2DATA, "squared visibility", Types.TYPE_DBL, this));

    // VIS2ERR  column definition
    addColumnMeta(new WaveColumnMeta(OIFitsConstants.COLUMN_VIS2ERR, "error in squared visibility", Types.TYPE_DBL, this));

    // UCOORD  column definition
    addColumnMeta(COLUMN_UCOORD);

    // VCOORD  column definition
    addColumnMeta(COLUMN_VCOORD);

    // STA_INDEX  column definition
    addColumnMeta(new ColumnMeta(OIFitsConstants.COLUMN_STA_INDEX, "station numbers contributing to the data", Types.TYPE_INT, 2) {

      @Override
      public short[] getIntAcceptedValues() {
        return getAcceptedStaIndexes();
      }
    });

    // FLAG  column definition
    addColumnMeta(new WaveColumnMeta(OIFitsConstants.COLUMN_FLAG, "flag", Types.TYPE_LOGICAL, this));
  }

  /* --- Columns --- */
  /**
   * Return the VIS2DATA column.
   * @return the VIS2DATA column.
   */
  public double[][] getVis2Data() {
    return this.getColumnDoubles(OIFitsConstants.COLUMN_VIS2DATA);
  }

  /**
   * Return the VIS2ERR column.
   * @return the VIS2ERR column.
   */
  public double[][] getVis2Err() {
    return this.getColumnDoubles(OIFitsConstants.COLUMN_VIS2ERR);
  }

  /**
   * Return the UCOORD column.
   * @return the UCOORD column.
   */
  public double[] getUCoord() {
    return this.getColumnDouble(OIFitsConstants.COLUMN_UCOORD);
  }

  /**
   * Return the VCOORD column.
   * @return the VCOORD column.
   */
  public double[] getVCoord() {
    return this.getColumnDouble(OIFitsConstants.COLUMN_VCOORD);
  }

  /* --- Alternate data representation methods --- */
  /**
   * Return the spacial frequencies column.  The computation is based
   * on ucoord and vcoord.
   * sqrt(ucoord^2+vcoord^2)/effWave
   *
   * @return the computed spacial frequencies r[x][y] (x,y for coordIndex,effWaveIndex)
   */
  public double[][] getSpacialFreq() {
    final double[][] r = new double[getNbRows()][getNWave()];
    final float[] effWaves = getOiWavelength().getEffWave();
    final double[] ucoord = getUCoord();
    final double[] vcoord = getVCoord();

    for (int i = 0, sizeU = ucoord.length; i < sizeU; i++) {
      for (int j = 0, sizeV = vcoord.length; j < sizeV; j++) {
        r[i][j] = (Math.sqrt((ucoord[i] * ucoord[i]) + (vcoord[i] * vcoord[i]))) / effWaves[j];
      }
    }

    return r;
  }

  /**
   * Return the spacial ucoord.
   * ucoord/effWave
   *
   * @return the computed spacial coords r[x][y] (x,y for coordIndex,effWaveIndex) .
   */
  public double[][] getSpacialUCoord() {
    return getSpacialCoord(getUCoord());
  }

  /**
   * Return the spacial vcoord.
   * vcoord/effWave
   *
   * @return the computed spacial coords r[x][y] (x,y for coordIndex,effWaveIndex) .
   */
  public double[][] getSpacialVCoord() {
    return getSpacialCoord(getVCoord());
  }
}
/*___oOo___*/
