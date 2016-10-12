package de.act.centralserver.applayer.agent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.act.central.types.Addresses;
import de.act.central.types.Attachment;
import de.act.central.types.BookingFlight;
import de.act.central.types.FormularInformation;
import de.act.central.types.Manifest;
import de.act.central.types.PackageItemBKG;
import de.act.central.types.Routing;
import de.act.common.types.formulars.FormularType;
import de.act.common.types.nonstaticobjects.RFForm;

/**
 * @author Martin Sachs & Henry
 * @since 1.0 - 07.11.2006
 * @update 02.11.2009
 */
public class ContentData implements Serializable, Cloneable {

	private static final long serialVersionUID = 1164784357908608807L;
	private RFForm form;
	private List<Attachment> attachments;
	private Manifest manifest;
	private FormularInformation formularInformations;
	/**
	 * @autor Martin Sachs
	 * @since 1.0 - 14.11.2006
	 * @return Returns the formularInformations.
	 */
	public final FormularInformation getFormularInformations() {
		return formularInformations;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 14.11.2006
	 * @param formularInformations The formularInformations to set.
	 */
	public final void setFormularInformations(FormularInformation formularInformations) {
		this.formularInformations = formularInformations;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param form
	 */
	public ContentData(FormularInformation form) {
		super();
		this.formularInformations = form;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param form
	 */
	public ContentData(RFForm form) {
		super();
		this.form = form;
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param manifest
	 */
	public ContentData(Manifest manifest) {
		super();
		this.manifest = manifest;
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 26.01.2007
	 * @param form
	 * @param attachments
	 * @param manifest
	 * @param formularInformations
	 */
	public ContentData(RFForm form, List<Attachment> attachments, Manifest manifest, FormularInformation formularInformations) {
		super();
		this.form = form;
		this.attachments = attachments;
		this.manifest = manifest;
		this.formularInformations = formularInformations;
	}
	/**
	 * @autor Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @return Returns the attachments.
	 */
	public final List<Attachment> getAttachments() {
		return attachments;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param attachments The attachments to set.
	 */
	public final void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}
	/**
	 * @autor Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @return Returns the form.
	 */
	public final RFForm getForm() {
		return form;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param form The form to set.
	 */
	public final void setForm(RFForm form) {
		this.form = form;
	}
	/**
	 * @autor Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @return Returns the manifest.
	 */
	public final Manifest getManifest() {
		return manifest;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param manifest The manifest to set.
	 */
	public final void setManifest(Manifest manifest) {
		this.manifest = manifest;
	}


	/**
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @return
	 * @return RSAdd
	 */
	public Integer getAgent() {
		Attachment a = getAttachmentfor(FormularType.ADDRESS);
		if (a!=null) {
			Set<Addresses> adds = a.getFAdds();
			if(adds!=null && adds.size()>0) {
				Addresses add = adds.iterator().next();
				return add.getFfwAddId();
			}
		}
		return null;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @param address
	 * @return
	 * @return Attachment
	 */
	private Attachment getAttachmentfor(FormularType address) {
		for (Attachment a:this.attachments) {
			if (a!=null && a.getAttType().equals(address.getConstaint())){
				return a;
			}
		}
		return null;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @return
	 * @return RSAdd
	 */
	public Integer getConsignee() {
		Attachment a = getAttachmentfor(FormularType.ADDRESS);
		if (a!=null) {
			Set<Addresses> adds = a.getFAdds();
			if(adds!=null && adds.size()>0) {
				Addresses add = adds.iterator().next();
				return add.getConAddId();
			}
		}
		return null;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @return
	 * @return Object
	 */
	public Integer getShipper() {
		Attachment a = getAttachmentfor(FormularType.ADDRESS);
		if (a!=null) {
			Set<Addresses> adds = a.getFAdds();
			if(adds!=null && adds.size()>0) {
				Addresses add = adds.iterator().next();
				return add.getShpAddId();
			}
		}
		return null;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @return
	 * @return List<RSAp>
	 */
	public List<String> getRouting() {
		Attachment a = getAttachmentfor(FormularType.ROUTING);

		if (a!=null) {
			List<String> list = new ArrayList<String>();
			Set<Routing> rt = a.getFRts();
			if (rt!=null) {
				for(Routing r:rt){
					list.add(r.getAp3lc());
				}
				return list;
			}
		}
		return null;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @return
	 * @return PackageItemBKG
	 */
	public PackageItemBKG getBookingItem() {
		Attachment a = getAttachmentfor(FormularType.PACKAGE);

		if (a!=null) {
			Set<PackageItemBKG> items = a.getFBkgItems();
			if (items!=null) {
				return items.iterator().next();
			}
		}
		return null;
	}
	/**
	 * @author Martin Sachs
	 * @since 1.0 - 05.12.2006
	 * @return
	 * @return Set<BookingFlight>
	 */
	public Set<BookingFlight> getFBkgFlts() {
		Attachment a = getAttachmentfor(FormularType.BKG_FLIGHT);

		if (a!=null) {
			Set<BookingFlight> items = a.getFBkgFlts();
			return items;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {

		RFForm form_clone = null;
		List<Attachment> attachments_clone= null;
		Manifest manifest_clone= null;
		FormularInformation formularInformations_clone= null;

		if (form!=null )
			form_clone = form; // dont need to clone, 'cause this is the orginal version from sender
		if (manifest!=null )
			manifest_clone = (Manifest) manifest; // TODO: Later, if manifest must change from box to box
		if (formularInformations!=null )
			formularInformations_clone = (FormularInformation) formularInformations.clone();

		if (attachments!=null) {
			attachments_clone = new ArrayList<Attachment>();
			attachments_clone.addAll(attachments);
		}
		ContentData ret = new ContentData(form_clone,attachments_clone,manifest_clone,formularInformations_clone);

		return ret;
	}
}
