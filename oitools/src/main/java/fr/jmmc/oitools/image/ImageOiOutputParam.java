/* 
 * Copyright (C) 2018 CNRS - JMMC project ( http://www.jmmc.fr )
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oitools.image;

import fr.jmmc.oitools.fits.FitsTable;

/**
 * This class is a container for IMAGE-OI OUTPUT PARAM.
 * https://github.com/emmt/OI-Imaging-JRA
 * It is returned be processing software and included in IMAGE-OI compliant files.
 *
 * @author mellag
 */
public final class ImageOiOutputParam extends FitsTable {

    // Image parameters
    public ImageOiOutputParam() {
        super();

        // TODO add standard keywords

        // Set default values
        setNbRows(0);
        setExtVer(1);
        setExtName(ImageOiConstants.EXTNAME_IMAGE_OI_OUTPUT_PARAM);
    }

}
