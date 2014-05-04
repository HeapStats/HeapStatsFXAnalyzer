/*
 * Copyright (C) 2014 Yasumasa Suenaga
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package jp.co.ntt.oss.heapstats.utils;

/**
 * Helper class for Information dialog.
 * 
 * @author Yasumasa Suenaga
 */
public class InfoDialog {
    
    private final String title;
    
    private final String message;
    
    private final String details;

    /**
     * Constructor of InfoDialog.
     * 
     * @param title Title which is shown in title bar.
     * @param message Main message which is shown in dialog.
     * @param details Detail message which is shown in dialog.
     */
    public InfoDialog(String title, String message, String details) {
        this.title = title;
        this.message = message;
        this.details = details;
    }
    
    /**
     * Show dialog.
     */
    public void show(){
        DialogHelper helper = new DialogHelper("/jp/co/ntt/oss/heapstats/utils/infoDialog.fxml", title);
        InfoDialogController controller = (InfoDialogController)helper.getController();
        controller.setMessage(message);
        controller.setDetails(details);
        
        helper.setOnShown((evt) -> controller.layout());
        
        helper.show();
    }
    
}
