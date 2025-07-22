package marcus.hansen

class BulkShipment(id: String, createdTimestamp: Long) : Shipment(id, ShipmentType.BULK, createdTimestamp)