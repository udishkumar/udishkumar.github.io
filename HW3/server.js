const express = require('express');
const path = require('path');
const axios = require('axios');
const app = express();
const PORT = parseInt(process.env.PORT)|| 8080;
const cors = require('cors');
const EBAY_URL = "https://svcs.ebay.com/services/search/FindingService/v1";
const EBAY_API_KEY = "UdishKum-pythonDe-PRD-a72a98ced-f8556164";
const MERCHANDISING_URL = "https://svcs.ebay.com/MerchandisingService";
const GOOGLE_API_ENDPOINT = "https://www.googleapis.com/customsearch/v1";
const CX = "d3c1ec159f9a3464d";
const GOOGLE_API_KEY = "AIzaSyBmdG2Av2i6SZD08ycWv9orPK-UaXzPOFg";
const AUTH_KEY="VWRpc2hLdW0tcHl0aG9uRGUtUFJELWE3MmE5OGNlZC1mODU1NjE2NDpQUkQtNzJhOThjZWQyZWM5LTMyYWEtNDc2OS1hNjA5LWVkOTU="


const { MongoClient } = require("mongodb");
const encodedPassword = encodeURIComponent("Karma@181818181");
const uri = `mongodb+srv://udishkum:${encodedPassword}@cluster0.2gusddi.mongodb.net/?retryWrites=true&w=majority`;
const client = new MongoClient(uri, { useNewUrlParser: true, useUnifiedTopology: true });
app.use(express.json());
app.use(cors());


app.get('/fetchPostalCodes', async (req, res) => {
    const zipCodeStartsWith = req.query.zipCodeStartsWith;

    if (!zipCodeStartsWith|| isNaN(zipCodeStartsWith)) {
        return res.status(200).json([]);
    }

    try {
        const response = await axios.get(`http://api.geonames.org/postalCodeSearchJSON?postalcode_startsWith=${zipCodeStartsWith}&maxRows=5&username=udishkumar&country=US`);
        const postalCodes = response.data.postalCodes.map(item => item.postalCode);
        res.json(postalCodes);
    } catch (error) {
        return res.status(200).json([]);
    }
});

app.get('/getEbayData', async (req, res) => {
    try {
        if (!req.query.keywords) { 
            return res.status(400).json({
                "eBayResponse": {
                    "responseCode": "1004",
                    "responseMsg": "Missing 'keywords' parameter. Please provide a keyword for search."
                }
            });
        }
        let keywords = decodeURIComponent(req.query.keywords);
        let currency = req.query.currency || 'USD';
        let payload = {
            "OPERATION-NAME": "findItemsAdvanced",
            "SERVICE-VERSION": "1.0.0",
            "SECURITY-APPNAME": EBAY_API_KEY,
            "RESPONSE-DATA-FORMAT": "JSON",
            "REST-PAYLOAD": "true",
            "keywords": keywords,
            "outputSelector(0)": "SellerInfo",
            "outputSelector(1)": "StoreInfo",
            "paginationInput.entriesPerPage":50,
            "buyerPostalCode": req.query.zipCode,
            "categoryId": req.query.category
        };
        let sortOrder = req.query.sortOrder;
        if (sortOrder) {
            payload.sortOrder = sortOrder;
        }

        let filter_counter = 0;

        const filter_params = [
            { argName: 'returnsAcceptedOnly', ebayName: 'ReturnsAcceptedOnly' },
            { argName: 'freeShippingOnly', ebayName: 'FreeShippingOnly' },
            { argName: 'localPickupOnly', ebayName: 'LocalPickupOnly' },
            { argName: 'hideDuplicateItems', ebayName: 'HideDuplicateItems', defaultValue: true },
            { argName: 'maxDistance', ebayName: 'MaxDistance', defaultValue: 10 }
        ];

        const condition_values = req.query.condition;
        if (condition_values) {
            const condition_values_list = condition_values.split(',');
            payload[`itemFilter(${filter_counter}).name`] = "Condition";
            condition_values_list.forEach((value, idx) => {
                payload[`itemFilter(${filter_counter}).value(${idx})`] = value;
            });
            filter_counter++;
        }

        filter_params.forEach(param => {
            const value = req.query[param.argName] || param.defaultValue;
            if (value) {
                payload[`itemFilter(${filter_counter}).name`] = param.ebayName;
                payload[`itemFilter(${filter_counter}).value`] = value;
                filter_counter++;
            }
        });

        const price_params = [
            { argName: 'minPrice', ebayName: 'MinPrice' },
            { argName: 'maxPrice', ebayName: 'MaxPrice' }
        ];

        price_params.forEach(param => {
            const value = req.query[param.argName];
            if (value) {
                payload[`itemFilter(${filter_counter}).name`] = param.ebayName;
                payload[`itemFilter(${filter_counter}).value`] = value;
                payload[`itemFilter(${filter_counter}).paramName`] = "Currency";
                payload[`itemFilter(${filter_counter}).paramValue`] = currency;
                filter_counter++;
            }
        });

        let limit = req.query.limit;
        if (limit) {
            limit = parseInt(limit);
            if (isNaN(limit)) {
                return res.status(400).json({
                    "eBayResponse": {
                        "responseCode": "1003",
                        "responseMsg": "Invalid 'limit' parameter. Please provide a numeric value."
                    }
                });
            }
        }
        const response = await axios.get(EBAY_URL, { params: payload });
        const data = response.data;

        const ack_status = data?.findItemsAdvancedResponse?.[0]?.ack?.[0];
        if (ack_status === "Failure") {
                const ebay_error = data?.findItemsAdvancedResponse?.[0]?.errorMessage?.[0]?.error?.[0];
                if (ebay_error) {
                    const response_code = ebay_error.errorId?.[0];
                    const response_msg = ebay_error.message?.[0];
                    return res.status(400).json({
                        "eBayResponse": {
                            "responseCode": response_code,
                            "responseMsg": 'Technical error encountered. ' + response_msg
                        }
                    });
                }
            }
    
            const total_entries = parseInt(data.findItemsAdvancedResponse[0].paginationOutput[0].totalEntries[0]);
            if (!total_entries || total_entries === 0) {
                return res.json({
                    "eBayResponse": {
                        "responseCode": "1001",
                        "responseMsg": "No results found for given search criteria"
                    }
                });
            }
    
            const items_data = data.findItemsAdvancedResponse[0].searchResult[0].item;
            let ebay_response_items = items_data.map(item => {
                const handlingTimeValue = item.shippingInfo?.[0]?.handlingTime?.[0];
                const formattedHandlingTime = handlingTimeValue ? (parseInt(handlingTimeValue) <= 1 ? handlingTimeValue + " Day" : handlingTimeValue + " Days") : "";
                return {
                    "product_id": item.itemId?.[0] || "",
                    "product_name": item.title?.[0] || "",
                    "product_image": item.galleryURL?.[0] || "",
                    "product_url": item.viewItemURL?.[0] || "",
                    "product_category": item.primaryCategory?.[0]?.categoryName?.[0] || "",
                    "product_condition": item.condition?.[0]?.conditionDisplayName?.[0] || "",
                    "product_price": item.sellingStatus?.[0]?.currentPrice?.[0]?.__value__ || "",
                    "top_rated": item.topRatedListing?.[0] || "",
                    "shipping_cost": (item.shippingInfo?.[0]?.shippingServiceCost?.[0]?.__value__ >= 0.01) ? item.shippingInfo[0].shippingServiceCost[0].__value__ : "",
                    "shipping_locations":item.shippingInfo?.[0]?.shipToLocations?.[0] || "",
                    "handling_time":formattedHandlingTime || "",
                    "expedited_shipping":item.shippingInfo?.[0]?.expeditedShipping?.[0] || "",
                    "one_day_shipping":item.shippingInfo?.[0]?.oneDayShippingAvailable?.[0] || "",
                    "return_accepted":item.returnsAccepted?.[0] || "",
                    "feedback_score":item.sellerInfo?.[0]?.feedbackScore?.[0] || "",
                    "popularity":item.sellerInfo?.[0]?.positiveFeedbackPercent?.[0] || "",
                    "feedback_rating_star":item.sellerInfo?.[0]?.feedbackRatingStar?.[0] || "",
                    "top_rated":item.sellerInfo?.[0]?.topRatedSeller?.[0] || "",
                    "store_name":item.storeInfo?.[0]?.storeName?.[0] || "",
                    "buy_product_at":item.storeInfo?.[0]?.storeURL?.[0] || "",
                    "shippingType": item.shippingInfo?.[0]?.shippingType?.[0] || "",
                    "zipCode": item.postalCode?.[0] || "",
                    "seller": (item.sellerInfo?.[0]?.sellerUserName?.[0] || "").toUpperCase() || ""
                };
            });
    
            if (limit !== undefined) {
                ebay_response_items = ebay_response_items.slice(0, limit);
            }

        return res.json(ebay_response_items);

    } catch (error) {
        return res.status(error?.response?.status || 500).json({
            "eBayResponse": {
                "responseCode": "1002",
                "responseMsg": "Failed to fetch data from eBay Finding API"
            }
        });
    }
});

app.get('/getEbayData/:item_id', async (req, res) => {
    try {
        const TOKEN_ENDPOINT = "https://api.ebay.com/identity/v1/oauth2/token";
        const headers = {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Authorization': 'Basic ' + AUTH_KEY
        };
        const data = `grant_type=client_credentials&scope=https://api.ebay.com/oauth/api_scope`;
        const tokenResponse = await axios.post(TOKEN_ENDPOINT, data, { headers });
        const access_token = tokenResponse.data.access_token;
        const SHOPPING_URL = "https://open.api.ebay.com/shopping";
        const header = {
            'X-EBAY-API-IAF-TOKEN': access_token
        };
        const payload = {
            callname: 'GetSingleItem',
            siteid: '0',
            version: '967',
            ItemID: req.params.item_id,
            IncludeSelector: 'Description,Details,ItemSpecifics',
            responseencoding: 'JSON',
            appid: EBAY_API_KEY
        };

        const payloadMerchandise = {
            "OPERATION-NAME": "getSimilarItems",
            "SERVICE-NAME": "MerchandisingService",
            "SERVICE-VERSION": "1.0.0",
            "CONSUMER-ID": "UdishKum-pythonDe-PRD-a72a98ced-f8556164",
            "RESPONSE-DATA-FORMAT": "JSON",
            "REST-PAYLOAD": "true",
            "maxResults": "20",
            "itemId": req.params.item_id
        };

        const [response, merchandiseResponse] = await Promise.all([
            axios.get(SHOPPING_URL, { headers: header, params: payload }),
            axios.get(MERCHANDISING_URL, { params: payloadMerchandise })
        ]);

        const responseData = response.data;
        const item = responseData.Item || {};
        const itemSpecifics = item.ItemSpecifics ? item.ItemSpecifics.NameValueList.map(nv => ({
            Name: nv.Name || "",
            Value: Array.isArray(nv.Value) ? nv.Value[0] : nv.Value || ""
        })) : [];
        const productImage = item.PictureURL || [];
        const currentPrice = item.CurrentPrice || {};
        const returnPolicy = item.ReturnPolicy || {};
        const ebayResponse = {
            product_id: item.ItemID || "",
            product_images: productImage,
            product_url: item.ViewItemURLForNaturalSearch || "",
            product_name: item.Title || "",
            product_price: '$'+`${currentPrice.Value || ''}`,
            location: item.PostalCode ? `${item.Location || ''}, ${item.PostalCode}` : item.Location || "",
            seller: item.Seller ? item.Seller.UserID : "",
            returnPolicy: `${returnPolicy.ReturnsAccepted || ''} within ${returnPolicy.ReturnsWithin || ''}`,
            refundMode: returnPolicy.Refund || "",
            returnsWithinDuration: returnPolicy.ReturnsWithin || "",
            returnsAccepted: returnPolicy.ReturnsAccepted || "",
            shippingCostPaidBy: returnPolicy.ShippingCostPaidBy || "",
            globalShipping: item.hasOwnProperty('GlobalShipping') ? item.GlobalShipping : "Not Available",
            handlingTime: item.HandlingTime || "",
            storeName: item.Storefront?.StoreName || "",
            storeURL: item.Storefront?.StoreURL || "",
            itemSpecifics
        };

        const color = '#325e5d';
        const similarItems = merchandiseResponse.data;
        const items = similarItems.getSimilarItemsResponse.itemRecommendations.item;
        const transformedItems = items.map(item => ({
            product_id: item.itemId || '',
            image_url: item.imageURL || '',
            product_name: item.title || '',
            product_url: item.viewItemURL || '',
            price: item.buyItNowPrice?.["__value__"] || '',
            shipping_cost: item.shippingCost?.["__value__"] || '',
            daysLeft: item.timeLeft ? item.timeLeft.match(/P(\d+)D/)?.[1] || '' : ''
        }));

        const finalResponse = {
            ...ebayResponse,
            similarItems: transformedItems
        };

        return res.json(finalResponse);

    } catch (error) {
        return res.status(error?.response?.status || 500).json({
            "eBayResponse": {
                "responseCode": "1008",
                "responseMsg": "Failed to fetch data from eBay APIs"
            }
        });
    }
});

app.get('/getEbayMerchandiseData/:itemID', async (req, res) => {
    try {
        const payload = {
            "OPERATION-NAME": "getSimilarItems",
            "SERVICE-NAME": "MerchandisingService",
            "SERVICE-VERSION": "1.0.0",
            "CONSUMER-ID": "UdishKum-pythonDe-PRD-a72a98ced-f8556164",
            "RESPONSE-DATA-FORMAT": "JSON",
            "REST-PAYLOAD": "true",
            "maxResults": "20",
            "itemId": req.params.itemID
        };

        const response = await axios.get(MERCHANDISING_URL, { params: payload });
        const data = response.data;

        if (data.getSimilarItemsResponse.ack !== "Success") {
            return res.status(400).json({
                "eBayResponse": {
                    "responseCode": "1007",
                    "responseMsg": "Failed to fetch data from eBay Merchandising API"
                }
            });
        }

        const color = '#325e5d';
        const items = data.getSimilarItemsResponse.itemRecommendations.item;
        const transformedItems = items.map(item => ({
            product_id: item.itemId || '',
            image_url: item.imageURL || '',
            product_name: item.title || '',
            product_url: item.viewItemURL || '',
            price: item.buyItNowPrice?.["__value__"] || '',
            shipping_cost: item.shippingCost?.["__value__"] || '',
            daysLeft: item.timeLeft ? item.timeLeft.match(/P(\d+)D/)?.[1] || '' : ''
        }));

        return res.json(transformedItems);
    } catch (error) {
        return res.status(error?.response?.status || 500).json({
            "eBayResponse": {
                "responseCode": "1007",
                "responseMsg": "Failed to fetch data from eBay Merchandising API"
            }
        });
    }
});

app.get('/getPhotos/:product_name', async (req, res) => {
    const productName = req.params.product_name;
    const formattedResponse = {};
    try {
        const response = await axios.get(GOOGLE_API_ENDPOINT, {
            params: {
                q: productName,
                cx: CX,
                imgSize: "huge",
                imgType: "photo",
                num: 8,
                searchType: "image",
                key: GOOGLE_API_KEY
            }
        });

        const links = response.data.items.map(item => item.link);
        
        links.forEach((link, index) => {
            formattedResponse[`link${index + 1}`] = link;
        });

        res.json(formattedResponse);

    } catch (error) {
        res.status(200);
        res.json(formattedResponse);
    }
});

let myDB;
let myColl;

client.connect()
    .then(() => {
        console.log("Connected successfully to Mongo Database Server");
        myDB = client.db('HW3');
        myColl = myDB.collection('wishlist');
        myColl.createIndex({ "product_id": 1 });
    })
    .catch(err => {
        console.error("Database connection error:", err);
        process.exit();
    });

app.post('/wishlist', async (req, res) => {
    try {
        const product = {
            product_id: req.body.product_id,
            product_image: req.body.product_image,
            product_name: req.body.product_name,
            product_price: req.body.product_price,
            shippingType: req.body.shippingType
        };

        await myColl.insertOne(product);
        res.status(201).send({message:"product added"});
    } catch (error) {
        console.error("Error:", error);
        res.status(500).send(error);
    }
});

app.delete('/wishlist/:product_id', async (req, res) => {
    try {
        const result = await myColl.deleteOne({ product_id: req.params.product_id });

        if (result.deletedCount === 0) return res.status(200).send({message: "Product not found"});
        res.status(200).send({ message: 'Product removed' });
    } catch (error) {
        console.error("Error:", error);
        res.status(500).send();
    }
});

app.get('/wishlist/:product_id', async (req, res) => {
    try {
        const product = await myColl.findOne({ product_id: req.params.product_id });

        if (!product) return res.status(200).send({message: "Product not found"});
        res.status(200).send(product);
    } catch (error) {
        console.error("Error:", error);
        res.status(500).send();
    }
});

app.get('/wishlist', async (req, res) => {
    try {
        const products = await myColl.find().toArray(); 
        res.status(200).send(products);
    } catch (error) {
        console.error("Error:", error);
        res.status(500).send();
    }
});


app.use('/', express.static(path.join(__dirname, 'dist/udishkumar-hw3')));
app.get('/*', (req, res) => {
    res.sendFile(path.join(__dirname, 'dist/udishkumar-hw3/index.html'));
});

app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).send('Something broke!');
});

app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
});