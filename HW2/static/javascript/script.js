let minPriceValue = 0;
let maxPriceValue = 0;
let displayCount = 3;
let currentSearchResults = null;
let currentSearchKeyword = "";
let currentSearchCount = 0;
let mainDiv = document.getElementById('mainDiv');
let formattedProductPrice=0
let formattedShippingCost=0
document.getElementById("searchForm").addEventListener("submit", function(event) {
    event.preventDefault();
    logWithUUID("Fetching form data... and preventing default form validation")
    logWithUUID("calling custom form validation...")
    validatePriceRange();
});

function validatePriceRange() {
    event.preventDefault();
    logWithUUID("custom form validation starts...")
    minPriceValue = parseFloat(document.getElementById('minId').value);
    maxPriceValue = parseFloat(document.getElementById('maxId').value);
    if (minPriceValue < 0 || maxPriceValue < 0) {
        alert("Price range values cannot be negative! Please try a value greater than or equal to 0.");
        logWithUUID("Price range values cannot be negative! Please try a value greater than or equal to 0.");
        return false;
    }

    if (minPriceValue > maxPriceValue) {
        alert("Oops! Lower price limit cannot be greater than upper price limit! Please try again.");
        logWithUUID("Oops! Lower price limit cannot be greater than upper price limit! Please try again.");
        return false;
    }
    displayCount = 3;
    logWithUUID("custom form validation completed, proceeding to Flask APIs for requesting ebay data")
    fetchDataFromEbay();
    return false;
}


function fetchDataFromEbay() {
    logWithUUID("Ebay data fetch request initiated...");
    let xhr = new XMLHttpRequest();
    
    let keywords = document.getElementById('keyword-text').value;
    if(keywords != "") {
        let selectedOptionId = document.querySelector('#sortBy option:checked').id;
        let sortOrderMap = {
            'BM': 'BestMatch',
            'PHF': 'CurrentPriceHighest',
            'BSHF': 'PricePlusShippingHighest',
            'PSLF': 'PricePlusShippingLowest'
        };
        let sortOrder = sortOrderMap[selectedOptionId];
        let returnsAcceptedOnly = document.getElementById('returnAccepted').checked;
        let conditions = [
            { id: 'new', value: 1000 },
            { id: 'used', value: 3000 },
            { id: 'veryGood', value: 4000 },
            { id: 'good', value: 5000 },
            { id: 'acceptable', value: 6000 },
        ];

        let conditionValues = conditions.filter(cond => document.getElementById(cond.id).checked).map(cond => cond.value);
        let freeShippingOnly = document.getElementById('freeShipping').checked;

        let params = [];
        
        if (!isNaN(minPriceValue) && minPriceValue >= 0) {
            params.push(`minPrice=${minPriceValue}`);
        }
        
        if (!isNaN(maxPriceValue) && maxPriceValue >= 0) {
            params.push(`maxPrice=${maxPriceValue}`);
        }
        
        if (returnsAcceptedOnly) {
            params.push(`ReturnsAcceptedOnly=${returnsAcceptedOnly}`);
        }

        if (freeShippingOnly) {
            params.push(`freeShippingOnly=${freeShippingOnly}`);
        }

        if (conditionValues.length) {
            params.push(`condition=${conditionValues.join(',')}`);
        }

        params.push(`keywords=${keywords}`);
        params.push(`sortOrder=${sortOrder}`);
        
        let apiUrl = `https://myfirstpython-3979650766.wl.r.appspot.com/getEbayData?${params.join('&')}`;

    xhr.open('GET', apiUrl, true);
    xhr.onload = function() {
        if (this.status == 200) {
            let response = JSON.parse(this.responseText);
            logWithUUID('ebay data fetched successfully!');

            if (response.eBayResponse.responseCode) {
                displayNoResultsMessage();
                logWithUUID('No data fetched from ebay');
            } else {
                displayResults(response.eBayResponse, keywords, response.productCount);
                logWithUUID('ebay data populated...');
            }
        } else {
            displayNoResultsMessage();
            logWithUUID('No results found displayed on frontend');
        }
    }
    xhr.send();
    logWithUUID('ebay data fetch request sent')
    }
}

function displayNoResultsMessage() {
    logWithUUID('Creating No results found message');
    let prevOutput = document.querySelector(".output");
    if (prevOutput) {
        prevOutput.remove();
    }

    let outputDiv = document.createElement('div');
    outputDiv.className = "output";

    let message = document.createElement('h2');
    message.className = "no-results-message";
    message.innerText = "No Results found";
    outputDiv.appendChild(message);

    mainDiv.appendChild(outputDiv);
}


function displayResults(data, keyword, productCount) {
    currentSearchResults = data;
    currentSearchKeyword = keyword;
    currentSearchCount = productCount;
    let prevOutput = document.querySelector(".output");
    if (prevOutput) {
        prevOutput.remove();
    }
    let outputDiv = document.createElement('div');
    outputDiv.className = "output";
    let heading = document.createElement('h2');
    heading.className = "results-found-heading";
    heading.innerHTML = `${productCount} Results found for <i>${keyword}</i>`;
    outputDiv.appendChild(heading);

    let hr = document.createElement('hr');
    hr.className = "results-found-hr";
    outputDiv.appendChild(hr);

    let slicedData = data.slice(0, displayCount);

    slicedData.forEach(product => {
        let card = document.createElement('div');
        card.classList.add('product-card');
        card.addEventListener('click', function() {
           fetchProductDetails(product.product_id);
           logWithUUID('Product details fetched for product id: '+product.product_id);
        });
        let imgDiv = document.createElement('div');
        imgDiv.className = 'product-image';
        let img = document.createElement('img');
        img.src = product.product_image || "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTUaY8AD_x6kRMw6dHReqBbsbiCIo_hNKv4AOVphg67lw&s";
        imgDiv.appendChild(img);
        
        let detailsDiv = document.createElement('div');
        detailsDiv.className = 'product-details';
        let nameDiv = document.createElement('div');
        nameDiv.className = 'product-name';
        nameDiv.innerHTML = `<b class="bold-name">${product.product_name}</b>`;
        
        let categoryDiv = document.createElement('div');
        categoryDiv.className = 'category';
        categoryDiv.innerHTML = `Category: <i>${product.product_category}</i> `;
        let categoryImg = document.createElement('img');
        categoryImg.src = './static/styles/images/redirect.png';
        categoryDiv.appendChild(categoryImg); 
        categoryImg.addEventListener('click', function() {
            event.stopPropagation();
            logWithUUID('Redirecting to ebay product page');
            window.open(product.product_url, '_blank');
            logWithUUID('ebay product page opened in new tab for product id: '+product.product_id);
        });

        let conditionDiv = document.createElement('div');
        conditionDiv.className = 'condition';
        if (!product.product_condition) {
            conditionDiv.style.display = "none";
        }
        if (product.product_condition) {
            conditionDiv.innerText = `Condition: ${product.product_condition} `;
            if (product.top_rated === "true") {
                let topRatedImg = document.createElement('img');
                topRatedImg.src = './static/styles/images/topRatedImage.png';
                conditionDiv.appendChild(topRatedImg);
                logWithUUID('Top rated product image added for product id: '+product.product_id);
            }
            detailsDiv.appendChild(conditionDiv);
        }
    
        let priceDiv = document.createElement('div');
        formattedProductPrice = formatToOneDecimal(product.product_price);
        formattedShippingCost = formatToOneDecimal(product.shipping_cost);

        if (product.shipping_cost != "") {
            priceDiv.innerHTML = `<b>Price: $${formattedProductPrice} (+ $${formattedShippingCost} for shipping)</b>`;
            logWithUUID('fetched product cost including shipping cost for product id: '+product.product_id);
        } else {
            priceDiv.innerHTML = `<b>Price: $${formattedProductPrice}</b>`;
            logWithUUID('fetched product cost only for product id: '+product.product_id);
        }

        detailsDiv.append(nameDiv, categoryDiv, conditionDiv, priceDiv);

        card.append(imgDiv, detailsDiv);
        outputDiv.appendChild(card);
    });

    if (displayCount === 3 && data.length > 3) {
        outputDiv.appendChild(createButton("Show More", () => {
            displayCount = 10;
            displayResults(data, keyword, productCount);
            logWithUUID('Card list expanded');
        }));
    } else if (displayCount === 10) {
        outputDiv.appendChild(createButton("Show Less", () => {
            displayCount = 3;
            displayResults(data, keyword, productCount);
            logWithUUID('Card list collapsed');
        }));
    }
    mainDiv.appendChild(outputDiv);
}

function fetchProductDetails(productId) {
    logWithUUID('Fetching product details for product id: '+productId);
    let xhr = new XMLHttpRequest();
    xhr.open('GET', `https://myfirstpython-3979650766.wl.r.appspot.com/getEbayData/${productId}`, true);
    xhr.onload = function() {
        if (this.status == 200) {
            let response = JSON.parse(this.responseText);
            displayProductDetails(response);
        } else {
            displayNoResultsMessage();
        }
    };
    xhr.send();
}

function displayProductDetails(data) {
    logWithUUID('Displaying product details for product id: '+data.product_id);
    let prevOutput = document.querySelector(".output");
    if (prevOutput) {
        prevOutput.remove();
    }

    let outputDiv = document.createElement('div');
    outputDiv.className = "output";

    let heading = document.createElement('h1');
    heading.className = "item-details-heading";
    heading.innerText = "Item Details";
    outputDiv.appendChild(heading);

    let backButton = createBackButton("Back to search results", () => {
        displayResults(currentSearchResults, currentSearchKeyword, currentSearchCount);
        logWithUUID('Moving back to search results page');
    });

    outputDiv.appendChild(backButton);

    let table = document.createElement('table');
    table.className = "item-details-table";
    let fields = [
        { name: 'Photo', value: `<img id="inlinePhoto" src="${data.product_image}" alt="Photo" width="100">`},
        { name: 'eBay Link', value: `<a href="${data.product_url}" target="_blank">eBay Product Link</a>` },
        { name: 'Title', value: data.product_name },
        { name: 'Price', value: `${data.product_price}` },
        { name: 'Location', value: data.location },
        {name: 'Seller', value: data.seller},
        { name: 'Return Policy (US)', value: data.returnPolicy}
    ];

    if (data.itemSpecifics && Array.isArray(data.itemSpecifics)) {
        logWithUUID('Item specifications added for product id: '+data.product_id);
        data.itemSpecifics.forEach(spec => {
            fields.push({ name: spec.Name, value: spec.Value });
        });
    }

    fields.forEach(item => {
        let tr = document.createElement('tr');
        
        let tdName = document.createElement('td');
        tdName.innerHTML = `<strong>${item.name}</strong>`;
        tr.appendChild(tdName);
        
        let tdValue = document.createElement('td');
        tdValue.innerHTML = item.value;
        tr.appendChild(tdValue);
        
        table.appendChild(tr);
    });

    outputDiv.appendChild(table);
    mainDiv.appendChild(outputDiv);
}

function scrollToTop() {
    window.scrollTo({
        top: document.body.scrollHeight,
        behavior: "smooth"
    });
    logWithUUID('scrolling up');
}

function scrollToBottom() {
    window.scrollTo({
        top: document.body.scrollHeight,
        behavior: "smooth"
    });
    logWithUUID('scrolling down');
}

function createBackButton(text, onClick) {
    logWithUUID('Back to results button created');
    let backButton = document.createElement('button');
    backButton.innerText = text;
    backButton.addEventListener('click', onClick);
    return backButton;
    
}

function createButton(text, onClick) {
    let button = document.createElement('button');
    button.innerText = text;
    button.addEventListener('click', onClick);

    if (text === "Show More") {
        button.addEventListener('click', scrollToBottom);
        logWithUUID('Displaying Show more button now');
    } else if (text === "Show Less") {
        button.addEventListener('click', scrollToTop);
        logWithUUID('Displaying Show less button now');
    }
    return button;
}

function createButtonArea(data, keyword, productCount) {
    let buttonArea = document.createElement('div');
    buttonArea.className = "button-area";
    if (displayCount === 3 && data.length > 3) {
        buttonArea.appendChild(createButton("Show More", () => {
            displayCount = 10;
            displayResults(data, keyword, productCount);
        }));
    } else if (displayCount === 10) {
        buttonArea.appendChild(createButton("Show Less", () => {
            displayCount = 3;
            displayResults(data, keyword, productCount);
        }));
    }
    return buttonArea;
}

function generateUUID() { 
    let d = new Date().getTime();
    let d2 = (performance && performance.now && (performance.now()*1000)) || 0;
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        let r = Math.random() * 16;
        if(d > 0){
            r = (d + r)%16 | 0;
            d = Math.floor(d/16);
        } else {
            r = (d2 + r)%16 | 0;
            d2 = Math.floor(d2/16);
        }
        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
}

function logWithUUID(message) {
    const id = generateUUID();
    console.log(`[ID:${id}] ${message}`);
}

function formatToOneDecimal(value) {
    if (typeof value === "string") {
        value = parseFloat(value);
    }
    if (Number.isInteger(value)) {
        return value.toFixed(1);
    }
    return value.toString();
}


document.getElementById('clearBtn').addEventListener('click', () => {
    logWithUUID('Reset form to default values');
    location.reload();
});