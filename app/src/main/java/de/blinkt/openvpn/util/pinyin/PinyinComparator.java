package de.blinkt.openvpn.util.pinyin;




import java.util.Comparator;

import de.blinkt.openvpn.model.ContactBean;

/**
 * 
 * @author xiaanming
 *
 */
public class PinyinComparator implements Comparator<ContactBean> {

	public int compare(ContactBean o1, ContactBean o2) {
		if (o1.getSortLetters().equals("@")
				|| o2.getSortLetters().equals("#")) {
			return -1;
		} else if (o1.getSortLetters().equals("#")
				|| o2.getSortLetters().equals("@")) {
			return 1;
		} else {
			return o1.getSortLetters().compareTo(o2.getSortLetters());
		}
	}

}
