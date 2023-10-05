from flask import Flask, jsonify, render_template, request
import requests
import urllib.parse
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

EBAY_URL = "https://svcs.ebay.com/services/search/FindingService/v1"
client_id = "UdishKum-pythonDe-PRD-a72a98ced-f8556164"

@app.route('/', methods=['GET'])

def index():
    return render_template('index.html') 

@app.route('/getEbayData', methods=['GET'])
def get_ebay_data():
    keywords = urllib.parse.unquote(request.args.get('keywords'))
    currency = request.args.get('currency', 'USD')
    payload = {
        "OPERATION-NAME": "findItemsAdvanced",
        "SERVICE-VERSION": "1.0.0",
        "SECURITY-APPNAME": client_id,
        "RESPONSE-DATA-FORMAT": "JSON",
        "REST-PAYLOAD": "true",
        "keywords": keywords
    }

    sortOrder = request.args.get('sortOrder')
    if sortOrder:
        payload.update({"sortOrder": sortOrder})

    filter_counter = 0

    filter_params = [
        ('returnsAcceptedOnly', 'ReturnsAcceptedOnly'),
        ('freeShippingOnly', 'FreeShippingOnly'),
    ]

    condition_values = request.args.get('condition')
    if condition_values:
        condition_values_list = condition_values.split(',')
        payload[f"itemFilter({filter_counter}).name"] = "Condition"
        for idx, value in enumerate(condition_values_list):
            payload[f"itemFilter({filter_counter}).value({idx})"] = value
        filter_counter += 1

    for arg_name, ebay_name in filter_params:
        value = request.args.get(arg_name)
        if value:
            payload.update({
                f"itemFilter({filter_counter}).name": ebay_name,
                f"itemFilter({filter_counter}).value": value
            })
            filter_counter += 1

    price_params = [
        ('minPrice', 'MinPrice'),
        ('maxPrice', 'MaxPrice')
    ]

    for arg_name, ebay_name in price_params:
        value = request.args.get(arg_name)
        if value:
            payload.update({
                f"itemFilter({filter_counter}).name": ebay_name,
                f"itemFilter({filter_counter}).value": value,
                f"itemFilter({filter_counter}).paramName": "Currency",
                f"itemFilter({filter_counter}).paramValue": currency
            })
            filter_counter += 1

    
    limit = request.args.get('limit')
    if limit:
        try:
            limit = int(limit)
        except ValueError:
            return jsonify({"eBayResponse": {"responseCode": "1003", "responseMsg": "Invalid 'limit' parameter. Please provide a numeric value."}}), 400
    else:
        limit = None

    response = requests.get(EBAY_URL, params=payload)
    if response.status_code == 200:
        data = response.json()

        ack_status = data.get("findItemsAdvancedResponse", [{}])[0].get("ack", [""])[0]
        if ack_status == "Failure":
            ebay_error = data.get("findItemsAdvancedResponse", [{}])[0].get("errorMessage", [{}])[0].get("error", [{}])[0]
            if ebay_error:
                response_code = ebay_error.get("errorId", [""])[0]
                response_msg = ebay_error.get("message", [""])[0]
                return jsonify({"eBayResponse": {"responseCode": response_code, "responseMsg": 'Technical error encountered. '+response_msg}}), 400

        total_entries = int(data["findItemsAdvancedResponse"][0]["paginationOutput"][0]["totalEntries"][0])
        if not total_entries or total_entries == 0:
            return jsonify({"eBayResponse": {"responseCode": "1001", "responseMsg": "No results found for given search criteria"}}), 200
        
        items_data = data["findItemsAdvancedResponse"][0]["searchResult"][0]["item"]
        ebay_response_items = []

        for item in items_data:
                transformed_item = {
                    "product_id": item["itemId"][0] if "itemId" in item else "",
                    "product_name": item["title"][0] if "title" in item else "",
                    "product_image": item["galleryURL"][0] if "galleryURL" in item else "",
                    "product_url": item["viewItemURL"][0] if "viewItemURL" in item else "",
                    "product_category": item["primaryCategory"][0]["categoryName"][0] if "primaryCategory" in item else "",
                    "product_condition": item["condition"][0]["conditionDisplayName"][0] if "condition" in item else "",
                    "product_price": float(item["sellingStatus"][0]["currentPrice"][0]["__value__"]) if "sellingStatus" in item else "",
                    "top_rated": item["topRatedListing"][0] if "topRatedListing" in item else "",
                    "shipping_cost": float(item["shippingInfo"][0]["shippingServiceCost"][0]["__value__"]) if "shippingInfo" in item and "shippingServiceCost" in item["shippingInfo"][0] and float(item["shippingInfo"][0]["shippingServiceCost"][0]["__value__"]) >= 0.01 else ""
                }
                ebay_response_items.append(transformed_item)

        if limit is not None:
            ebay_response_items = ebay_response_items[:limit]
        else: pass

        transformed_response = {
            "productCount": total_entries,
            "keyword": urllib.parse.unquote(keywords),
            "eBayResponse": ebay_response_items
        }

        return jsonify(transformed_response)

    else:
        return jsonify({"eBayResponse": {"responseCode": "1002", "responseMsg": "Failed to fetch data from eBay Finding API"}}), response.status_code


@app.route('/getEbayData/<item_id>', methods=['GET'])
def get_ebay_data_by_id(item_id):
    TOKEN_ENDPOINT = "https://api.ebay.com/identity/v1/oauth2/token"
    headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': 'Basic VWRpc2hLdW0tcHl0aG9uRGUtUFJELWE3MmE5OGNlZC1mODU1NjE2NDpQUkQtNzJhOThjZWQyZWM5LTMyYWEtNDc2OS1hNjA5LWVkOTU='
    }
    data = {
        'grant_type': 'client_credentials',
        'scope': 'https://api.ebay.com/oauth/api_scope'
    }
    token_response = requests.post(TOKEN_ENDPOINT, headers=headers, data=data)
    
    if token_response.status_code != 200:
        return jsonify({"eBayResponse": {"responseCode": "1006", "responseMsg": "Failed to fetch data from eBay Shopping API"}}), token_response.status_code

    access_token = token_response.json().get('access_token')
    
    SHOPPING_URL = "https://open.api.ebay.com/shopping"
    header = {
        'X-EBAY-API-IAF-TOKEN': access_token
    }
    payload = {
        'callname': 'GetSingleItem',
        'siteid': '0',
        'version': '967',
        'ItemID': item_id,
        'IncludeSelector': 'Description,Details,ItemSpecifics',
        'responseencoding': 'JSON',
        'appid': client_id
    }
    
    response = requests.get(SHOPPING_URL, headers=header, params=payload)
    response_data = response.json()

    if response_data.get("Ack") == "Failure":
        ebay_error = response_data.get("Errors", [{}])[0]
        responseCode = ebay_error.get("ErrorCode", "")
        responseMessage = ebay_error.get("LongMessage", "")
        return jsonify({
            "eBayResponse": {
                "responseCode": responseCode,
                "responseMsg": responseMessage
            }
        }), 400

    item = response_data.get("Item", {})    
    item_specifics = [
        {
            "Name": nv.get("Name", ""),
            "Value": nv["Value"][0] if isinstance(nv.get("Value", None), list) else nv.get("Value", "")
        } for nv in item.get("ItemSpecifics", {}).get("NameValueList", [])
    ]

    product_image = item.get("PictureURL", [])
    ebay_response = {
        "product_id": item.get("ItemID", ""),
        "product_image": product_image[0] if product_image else "https://ir.ebaystatic.com/cr/v/c1/2018/flashCpn/social-share-default.jpg",
        "product_url": item.get("ViewItemURLForNaturalSearch", ""),
        "product_name": item.get("Title", ""),
        "product_price": f"{item.get('CurrentPrice', {}).get('Value', '')} {item.get('CurrentPrice', {}).get('CurrencyID', '')}",
        "location": f"{item.get('Location', '')}, {item.get('PostalCode', '')}" if item.get("PostalCode") else item.get("Location", ""),
        "seller": item.get("Seller", {}).get("UserID", ""),
        "returnPolicy": f"{item.get('ReturnPolicy', {}).get('ReturnsAccepted', '')} within {item.get('ReturnPolicy', {}).get('ReturnsWithin', '')}",
        "itemSpecifics": item_specifics
    }

    return jsonify(ebay_response)

if __name__ == '__main__':
    app.run(host="127.0.0.1",port=8080, debug=True)